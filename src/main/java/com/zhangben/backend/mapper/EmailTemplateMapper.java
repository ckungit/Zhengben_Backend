package com.zhangben.backend.mapper;

import com.zhangben.backend.model.EmailTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmailTemplateMapper {

    /**
     * 根据模板代码和语言查询（精确匹配）
     */
    @Select("SELECT * FROM email_template WHERE template_code = #{code} AND language = #{language} AND status = 1")
    @Results(id = "templateResultMap", value = {
        @Result(property = "templateCode", column = "template_code"),
        @Result(property = "templateName", column = "template_name"),
        @Result(property = "htmlContent", column = "html_content"),
        @Result(property = "variablesHint", column = "variables_hint"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    EmailTemplate selectByCodeAndLanguage(@Param("code") String code, @Param("language") String language);

    /**
     * 根据模板代码查询（默认语言 zh-CN）
     */
    @Select("SELECT * FROM email_template WHERE template_code = #{code} AND language = 'zh-CN' AND status = 1")
    @ResultMap("templateResultMap")
    EmailTemplate selectByCode(String code);

    /**
     * 查询所有模板（管理用）
     */
    @Select("SELECT * FROM email_template ORDER BY template_code, language")
    @ResultMap("templateResultMap")
    List<EmailTemplate> selectAll();

    /**
     * 按模板代码查询所有语言版本
     */
    @Select("SELECT * FROM email_template WHERE template_code = #{code} ORDER BY language")
    @ResultMap("templateResultMap")
    List<EmailTemplate> selectAllLanguagesByCode(String code);

    /**
     * 更新模板
     */
    @Update("UPDATE email_template SET subject = #{subject}, html_content = #{htmlContent}, " +
            "template_name = #{templateName}, description = #{description}, variables_hint = #{variablesHint}, " +
            "updated_at = NOW() WHERE template_code = #{templateCode} AND language = #{language}")
    int updateByCodeAndLanguage(EmailTemplate template);

    /**
     * 插入模板
     */
    @Insert("INSERT INTO email_template (template_code, language, template_name, subject, html_content, description, variables_hint, status) " +
            "VALUES (#{templateCode}, #{language}, #{templateName}, #{subject}, #{htmlContent}, #{description}, #{variablesHint}, 1)")
    int insert(EmailTemplate template);

    /**
     * 按ID删除
     */
    @Delete("DELETE FROM email_template WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 按ID查询
     */
    @Select("SELECT * FROM email_template WHERE id = #{id}")
    @ResultMap("templateResultMap")
    EmailTemplate selectById(Integer id);

    /**
     * 获取所有不同的模板代码
     */
    @Select("SELECT DISTINCT template_code FROM email_template ORDER BY template_code")
    List<String> selectDistinctCodes();
}
