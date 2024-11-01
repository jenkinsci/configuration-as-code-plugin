package io.jenkins.plugins.casc;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class MockHttpServletResponse implements HttpServletResponse {

    private int error = 200;

    @Override
    public void addCookie(Cookie cookie) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectURL(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String encodeRedirectUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int sc, String msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendError(int sc) {
        error = sc;
    }

    @Override
    public void sendRedirect(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDateHeader(String name, long date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDateHeader(String name, long date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeader(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addHeader(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIntHeader(String name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addIntHeader(String name, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int sc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(int sc, String sm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus() {
        return error;
    }

    @Override
    public String getHeader(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getHeaders(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getHeaderNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletOutputStream getOutputStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintWriter getWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLength(int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentLengthLong(long len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setContentType(String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBufferSize(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flushBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLocale(Locale loc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }
}
