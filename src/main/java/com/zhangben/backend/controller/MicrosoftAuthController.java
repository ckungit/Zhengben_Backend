package com.zhangben.backend.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.zhangben.backend.dto.GoogleSSOResponse;
import com.zhangben.backend.dto.CompleteProfileRequest;
import com.zhangben.backend.dto.LoginResponse;
import com.zhangben.backend.model.User;
import com.zhangben.backend.model.UserExample;
import com.zhangben.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.zhangben.backend.util.CurrencyUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * V43: Microsoft/Outlook SSO 控制器
 * 处理 Microsoft 账号的登录、注册和资料完善
 */
@RestController
@RequestMapping("/api/auth/microsoft")
public class MicrosoftAuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${microsoft.client-id:}")
    private String microsoftClientId;

    // Microsoft OIDC JWKS endpoint
    private static final String MS_JWKS_URL = "https://login.microsoftonline.com/common/discovery/v2.0/keys";

    /**
     * Microsoft SSO 登录
     * 前端使用 MSAL 获取 ID Token 后发送到此接口
     */
    @PostMapping("/login")
    public GoogleSSOResponse microsoftLogin(@RequestBody Map<String, String> request) throws Exception {
        if (microsoftClientId == null || microsoftClientId.isEmpty()) {
            throw new RuntimeException("Microsoft SSO 未配置");
        }

        String idTokenString = request.get("idToken");
        if (idTokenString == null || idTokenString.isEmpty()) {
            throw new RuntimeException("缺少 Microsoft ID Token");
        }

        // 验证 Microsoft ID Token
        JWTClaimsSet claims = verifyMicrosoftIdToken(idTokenString);

        String microsoftId = claims.getSubject();
        String email = claims.getStringClaim("email");
        if (email == null) {
            // 某些 Microsoft 账号可能使用 preferred_username
            email = claims.getStringClaim("preferred_username");
        }
        if (email == null) {
            throw new RuntimeException("无法获取 Microsoft 邮箱");
        }

        // 复用 GoogleSSOResponse（结构相同）
        GoogleSSOResponse response = new GoogleSSOResponse();
        response.setEmail(email);

        // 1. 按 microsoftId 查找（已绑定 Microsoft 的用户）
        User microsoftUser = userMapper.selectByMicrosoftId(microsoftId);

        if (microsoftUser != null) {
            // 已绑定 Microsoft 的用户
            if (microsoftUser.getProfileCompleted() == null || !microsoftUser.getProfileCompleted()) {
                StpUtil.login(microsoftUser.getId(), "microsoft-sso");
                CurrencyUtils.setSessionCurrency(microsoftUser.getPrimaryCurrency());

                response.setToken(StpUtil.getTokenValue());
                response.setIsNewUser(false);
                response.setNeedsProfileCompletion(true);
                response.setEmail(microsoftUser.getEmail());
            } else {
                StpUtil.login(microsoftUser.getId(), "microsoft-sso");
                CurrencyUtils.setSessionCurrency(microsoftUser.getPrimaryCurrency());

                response.setToken(StpUtil.getTokenValue());
                response.setIsNewUser(false);
                response.setNeedsProfileCompletion(false);
                response.setNickname(microsoftUser.getNickname());
                response.setRole(microsoftUser.getRole());
                response.setEmail(microsoftUser.getEmail());
            }
            return response;
        }

        // 2. 按邮箱查找（邮箱相同但未绑定 Microsoft 的用户）
        UserExample emailExample = new UserExample();
        emailExample.createCriteria().andEmailEqualTo(email);
        List<User> emailUsers = userMapper.selectByExample(emailExample);

        if (emailUsers.isEmpty()) {
            // 新用户 - 临时登录，进入注册流程
            StpUtil.login("ms_" + microsoftId, "microsoft-temp");
            StpUtil.getSession().set("microsoftId", microsoftId);
            StpUtil.getSession().set("microsoftEmail", email);

            response.setToken(StpUtil.getTokenValue());
            response.setIsNewUser(true);
            response.setNeedsProfileCompletion(true);
            return response;

        } else {
            User existingUser = emailUsers.get(0);

            // 邮箱相同 - 自动绑定 Microsoft ID 并登录
            User update = new User();
            update.setId(existingUser.getId());
            update.setMicrosoftId(microsoftId);
            userMapper.updateByPrimaryKeySelective(update);

            StpUtil.login(existingUser.getId(), "microsoft-sso");
            CurrencyUtils.setSessionCurrency(existingUser.getPrimaryCurrency());

            response.setToken(StpUtil.getTokenValue());
            response.setIsNewUser(false);
            response.setNeedsProfileCompletion(false);
            response.setNickname(existingUser.getNickname());
            response.setRole(existingUser.getRole());
        }

        return response;
    }

    /**
     * 完善 Microsoft 用户资料（新用户在此创建）
     */
    @PostMapping("/complete-profile")
    public LoginResponse completeProfile(@RequestBody CompleteProfileRequest request) {
        StpUtil.checkLogin();

        String loginDevice = StpUtil.getLoginDevice();

        if (!"microsoft-temp".equals(loginDevice)) {
            throw new RuntimeException("当前不是 Microsoft 注册流程");
        }

        // 验证输入
        if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
            throw new RuntimeException("昵称不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new RuntimeException("密码至少8位");
        }

        String microsoftId = (String) StpUtil.getSession().get("microsoftId");
        String microsoftEmail = (String) StpUtil.getSession().get("microsoftEmail");

        if (microsoftId == null || microsoftEmail == null) {
            throw new RuntimeException("Microsoft 登录信息缺失，请重新登录");
        }

        User user;

        // 检查是否已存在（防止重复）
        User existingUser = userMapper.selectByMicrosoftId(microsoftId);

        if (existingUser != null) {
            // 已存在，更新资料
            user = existingUser;
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setNickname(request.getNickname().trim());
            updateUser.setPassword(passwordEncoder.encode(request.getPassword()));
            updateUser.setSecondname(request.getSecondname());
            updateUser.setFirstname(request.getFirstname());
            updateUser.setPaypayFlag(request.getPaypayFlag() != null ? request.getPaypayFlag().byteValue() : 0);
            updateUser.setBankFlag(request.getBankFlag() != null ? request.getBankFlag().byteValue() : 0);
            updateUser.setProfileCompleted(true);
            userMapper.updateByPrimaryKeySelective(updateUser);
        } else {
            // 创建新用户
            user = new User();
            user.setEmail(microsoftEmail);
            user.setMicrosoftId(microsoftId);
            user.setNickname(request.getNickname().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setSecondname(request.getSecondname());
            user.setFirstname(request.getFirstname());
            user.setPaypayFlag(request.getPaypayFlag() != null ? request.getPaypayFlag().byteValue() : 0);
            user.setBankFlag(request.getBankFlag() != null ? request.getBankFlag().byteValue() : 0);
            user.setRole("user");
            user.setProfileCompleted(true);
            userMapper.insertSelective(user);
        }

        // 清除临时登录态
        StpUtil.logout();

        // 正式登录
        StpUtil.login(user.getId(), "microsoft-sso");
        CurrencyUtils.setSessionCurrency(user.getPrimaryCurrency());

        LoginResponse response = new LoginResponse();
        response.setToken(StpUtil.getTokenValue());
        response.setNickname(request.getNickname().trim());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        return response;
    }

    /**
     * 验证 Microsoft ID Token（使用 JWKS）
     */
    private JWTClaimsSet verifyMicrosoftIdToken(String idTokenString) throws Exception {
        // 配置 JWT 处理器
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        // 使用 Microsoft 的 JWKS 端点
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URI(MS_JWKS_URL).toURL());

        // RS256 是 Microsoft 使用的签名算法
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                JWSAlgorithm.RS256, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);

        // 验证并解析 token
        JWTClaimsSet claims = jwtProcessor.process(idTokenString, null);

        // 验证 audience (必须是我们的 client ID)
        if (!claims.getAudience().contains(microsoftClientId)) {
            throw new RuntimeException("Token audience 不匹配");
        }

        // 验证 issuer
        String issuer = claims.getIssuer();
        if (issuer == null ||
            (!issuer.startsWith("https://login.microsoftonline.com/") &&
             !issuer.startsWith("https://login.live.com/"))) {
            throw new RuntimeException("Token issuer 不合法");
        }

        return claims;
    }
}
