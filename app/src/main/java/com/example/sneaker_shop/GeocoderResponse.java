package com.example.sneaker_shop;

import java.util.List;

public class GeocoderResponse {
    public Response response;

    public static class Response {
        public GeoObjectCollection GeoObjectCollection;
    }

    public static class GeoObjectCollection {
        public List<FeatureMember> featureMember;
    }

    public static class FeatureMember {
        public GeoObject GeoObject;
    }

    public static class GeoObject {
        public Point Point;
        public String name;
    }

    public static class Point {
        public String pos;
    }
}
