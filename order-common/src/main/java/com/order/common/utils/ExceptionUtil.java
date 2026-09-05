package com.order.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 错误信息处理类。
 *
 * @author order
 */
public class ExceptionUtil
{
    private static final int MAX_CONCISE_MESSAGE_LENGTH = 1000;

    /**
     * 获取exception的详细错误信息。
     */
    public static String getExceptionMessage(Throwable e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    public static String getRootErrorMessage(Exception e)
    {
        Throwable root = ExceptionUtils.getRootCause(e);
        root = (root == null ? e : root);
        if (root == null)
        {
            return "";
        }
        String msg = root.getMessage();
        if (msg == null)
        {
            return "null";
        }
        return StringUtils.defaultString(msg);
    }

    /**
     * 获取适合单行日志输出的最深层错误信息，不包含异常堆栈。
     */
    public static String getConciseErrorMessage(Throwable e)
    {
        if (e == null)
        {
            return "Unknown error";
        }

        Throwable root = ExceptionUtils.getRootCause(e);
        root = (root == null ? e : root);
        String message = root.getMessage();
        if (StringUtils.isEmpty(message))
        {
            message = e.getMessage();
        }
        if (StringUtils.isEmpty(message))
        {
            return root.getClass().getSimpleName();
        }

        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() > MAX_CONCISE_MESSAGE_LENGTH)
        {
            normalized = normalized.substring(0, MAX_CONCISE_MESSAGE_LENGTH) + "...";
        }
        return root.getClass().getSimpleName() + ": " + normalized;
    }
}
