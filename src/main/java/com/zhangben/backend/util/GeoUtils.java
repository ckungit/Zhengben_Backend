package com.zhangben.backend.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBWriter;

public class GeoUtils {

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * 将经纬度转换为 MySQL POINT 的 WKB（二进制）格式
     */
    public static byte[] toPoint(double longitude, double latitude) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        WKBWriter writer = new WKBWriter();
        return writer.write(point);
    }
}