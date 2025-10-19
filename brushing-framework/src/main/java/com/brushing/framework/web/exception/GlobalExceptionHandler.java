package com.brushing.framework.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.brushing.common.constant.HttpStatus;
import com.brushing.common.core.domain.AjaxResult;
import com.brushing.common.core.text.Convert;
import com.brushing.common.exception.DemoModeException;
import com.brushing.common.exception.ServiceException;
import com.brushing.common.utils.StringUtils;
import com.brushing.common.utils.html.EscapeUtil;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.EOFException;
import java.net.SocketException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* -------------------- 辅助方法 -------------------- */

    /** 粗略判断是否为二进制资源请求（根据 URI 后缀 + Accept 头） */
    private boolean isBinaryRequest(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri != null && uri.matches("(?i).+\\.(png|jpg|jpeg|gif|webp|bmp|ico|svg|mp4|mp3|pdf)$")) {
            return true;
        }
        String accept = req.getHeader("Accept");
        return accept != null && (
                accept.toLowerCase().startsWith("image/") ||
                        accept.toLowerCase().startsWith("video/") ||
                        accept.toLowerCase().contains("application/pdf")
        );
    }

    /** 仅当可以安全写回 JSON 时才写；否则只记日志不回写（返回 null） */
    private ResponseEntity<AjaxResult> maybeWriteJson(Exception e,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response,
                                                      int httpStatus,
                                                      String message) {
        if (response.isCommitted() || isBinaryRequest(request)) {
            log.error("Error on committed/binary response, uri={}", request.getRequestURI(), e);
            return null;
        }
        AjaxResult body = AjaxResult.error(httpStatus, message);
        return ResponseEntity
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /* -------------------- 客户端断开类异常：忽略，不回写 -------------------- */

    @ExceptionHandler({ ClientAbortException.class, EOFException.class, SocketException.class })
    public void handleClientAbort(Exception e, HttpServletRequest request) {
        // 常见且可忽略：前端取消/页面跳转/网络抖动等
        if (log.isDebugEnabled()) {
            log.debug("Client aborted connection: uri={}, ex={}", request.getRequestURI(), e.toString());
        }
    }

    /* -------------------- 权限/请求方式/校验/业务等：可写 JSON 的情况 -------------------- */

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                          HttpServletRequest request,
                                                                          HttpServletResponse response) {
        log.error("URI='{}', 不支持'{}'请求", request.getRequestURI(), e.getMethod());
        return AjaxResult.error(e.getMessage());
    }

    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        log.error("ServiceException: {}", e.getMessage(), e);
        Integer code = e.getCode();
        int httpStatus = (code != null ? code : HttpStatus.ERROR);
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());

    }

    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e,
                                                                         HttpServletRequest request,
                                                                         HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                                                HttpServletRequest request,
                                                                                HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value));
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                            HttpServletRequest request,
                                                                            HttpServletResponse response) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }

    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return AjaxResult.error("演示模式，不允许操作");
    }


    /* -------------------- 运行时 & 兜底异常 -------------------- */

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AjaxResult> handleRuntimeException(RuntimeException e,
                                                             HttpServletRequest request,
                                                             HttpServletResponse response) {
        log.error("URI='{}', 运行时异常", request.getRequestURI(), e);
        return maybeWriteJson(e, request, response, HttpStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleException(Exception e,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        log.error("URI='{}', 系统异常", request.getRequestURI(), e);
        return maybeWriteJson(e, request, response, HttpStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoStatic(NoResourceFoundException e, HttpServletRequest req) {
        log.warn("Static resource not found, uri={}, path={}", req.getRequestURI(), e.getResourcePath());
        // 不回写 JSON（图片请求），直接 404
        return ResponseEntity.status(404).build();
    }
}
