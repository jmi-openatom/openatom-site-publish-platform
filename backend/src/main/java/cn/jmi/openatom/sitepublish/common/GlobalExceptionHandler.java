package cn.jmi.openatom.sitepublish.common;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus()).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("登录状态已失效，请重新登录"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        String message;
        if (exception instanceof MethodArgumentNotValidException validException) {
            message = Optional.ofNullable(validException.getBindingResult().getFieldError())
                    .map(error -> error.getDefaultMessage())
                    .orElse("请求参数不正确");
        } else {
            BindException bindException = (BindException) exception;
            message = Optional.ofNullable(bindException.getBindingResult().getFieldError())
                    .map(error -> error.getDefaultMessage())
                    .orElse("请求参数不正确");
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        String[] supportedMethods = exception.getSupportedMethods();
        String supported = supportedMethods == null || supportedMethods.length == 0
                ? ""
                : String.join(", ", supportedMethods);
        String message = supported.isBlank()
                ? "请求方法 " + request.getMethod() + " 不受支持"
                : "请求方法 " + request.getMethod() + " 不受支持，请使用 " + supported;

        log.warn("Method not allowed: {} {} (supported: {})",
                request.getMethod(), request.getRequestURI(), supported);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (supportedMethods != null && supportedMethods.length > 0) {
            response.header(HttpHeaders.ALLOW, supportedMethods);
        }
        return response.body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("服务器暂时无法完成请求"));
    }
}
