package com.madelmade.config;

public final class SafeRedirect {
    private SafeRedirect() {}

    /** Only allow redirects to paths on this site, never to another domain. */
    public static boolean isLocal(String url) {
        return url != null && url.startsWith("/") && !url.startsWith("//") && !url.contains("\\")
                && !url.contains("\r") && !url.contains("\n");
    }

    public static String orDefault(String url, String fallback) {
        return isLocal(url) ? url : fallback;
    }
}
