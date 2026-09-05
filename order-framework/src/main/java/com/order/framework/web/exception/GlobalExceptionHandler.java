package com.order.framework.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.order.common.constant.HttpStatus;
import com.order.common.core.domain.AjaxResult;
import com.order.common.core.text.Convert;
import com.order.common.exception.DemoModeException;
import com.order.common.exception.ServiceException;
import com.order.common.utils.ExceptionUtil;
import com.order.common.utils.StringUtils;
import com.order.common.utils.html.EscapeUtil;

/**
 * 全局异常处理器
 * 
 * @author order
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.warn("权限校验失败 path={} error={}", requestURI, ExceptionUtil.getConciseErrorMessage(e));
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.warn("请求方法不支持 path={} method={} error={}",
                requestURI, e.getMethod(), ExceptionUtil.getConciseErrorMessage(e));
        if (requestURI != null && requestURI.startsWith("/api/"))
        {
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
                    .body(AjaxResult.error(HttpStatus.BAD_METHOD, "Method not allowed"));
        }
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.warn("业务处理失败 path={} error={}",
                request.getRequestURI(), ExceptionUtil.getConciseErrorMessage(e));
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.warn("请求路径缺少必需变量 path={} error={}",
                requestURI, ExceptionUtil.getConciseErrorMessage(e));
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.warn("请求参数类型不匹配 path={} error={}",
                requestURI, ExceptionUtil.getConciseErrorMessage(e));
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value));
    }

    /**
     * 请求的路由或静态资源不存在。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException e,
            HttpServletRequest request)
    {
        log.warn("请求资源不存在 path={} error={}",
                request.getRequestURI(), ExceptionUtil.getConciseErrorMessage(e));
        return ResponseEntity.notFound().build();
    }

    /**
     * 客户端在资源传输完成前主动断开，不属于服务端故障。
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e, HttpServletRequest request)
    {
        log.debug("客户端中断请求 path={} error={}",
                request.getRequestURI(), ExceptionUtil.getConciseErrorMessage(e));
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求处理异常 path={} error={}",
                requestURI, ExceptionUtil.getConciseErrorMessage(e));
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求处理异常 path={} error={}",
                requestURI, ExceptionUtil.getConciseErrorMessage(e));
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.warn("请求参数绑定失败 error={}", ExceptionUtil.getConciseErrorMessage(e));
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.warn("请求参数校验失败 error={}", ExceptionUtil.getConciseErrorMessage(e));
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return AjaxResult.error("演示模式，不允许操作");
    }

}
