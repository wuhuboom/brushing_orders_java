package com.order.api.controller;

import com.order.api.controller.dto.AvatarDto;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.api.controller.dto.CheckTradePassword;
import com.order.api.controller.dto.EditPasswordDto;
import com.order.api.controller.dto.EditTradePasswordDto;
import com.order.api.controller.dto.LoginUserDto;
import com.order.api.controller.dto.RegisterDto;
import com.order.api.service.UserApiService;
import com.order.api.service.UserLoginResult;
import com.order.api.service.ApiLocaleService;
import com.order.api.service.LocalizedApiMessageService;
import com.order.common.core.controller.BaseController;
import com.order.common.core.domain.AjaxResult;
import com.order.common.i18n.SupportedLocale;
import com.order.common.config.OrderConfig;
import com.order.common.utils.file.FileUploadUtils;
import com.order.common.utils.file.ImageUploadValidator;
import com.order.common.utils.file.MimeTypeUtils;
import com.order.framework.config.ServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
public class AuthController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserApiService userApiService;
    private final ServerConfig serverConfig;
    private final ApiLocaleService localeService;
    private final LocalizedApiMessageService messageService;

    public AuthController(
            UserApiService userApiService,
            ServerConfig serverConfig,
            ApiLocaleService localeService,
            LocalizedApiMessageService messageService) {
        this.userApiService = userApiService;
        this.serverConfig = serverConfig;
        this.localeService = localeService;
        this.messageService = messageService;
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public AjaxResult login(
            @Valid @RequestBody LoginUserDto loginRequest,
            HttpServletRequest request) {
        UserLoginResult result = userApiService.login(loginRequest, request);
        return success(result);
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public AjaxResult register(@Valid @RequestBody RegisterDto registerRequest) {
        userApiService.register(registerRequest);
        return success("register success");
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public AjaxResult logout(HttpServletRequest request) {
        userApiService.logout(request.getHeader("Authorization"));
        return success("Logout successful");
    }

    @GetMapping("/getInfo")
    @Operation(summary = "获取用户信息")
    public ResponseEntity<AjaxResult> userInfo(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = SupportedLocale.resolve(
                lang, request.getHeader("Accept-Language"));
        AjaxResult result = AjaxResult.success(
                messageService.message(200, locale, "Success"),
                userApiService.userInfo(userId, locale));
        return ResponseEntity.ok().headers(localeService.responseHeaders(locale)).body(result);
    }

    @PostMapping("/updateAvatar")
    @Operation(summary = "添加或修改用户头像")
    public AjaxResult updateAvatar(
            @Valid @RequestBody AvatarDto request,
            @RequestAttribute("userId") Long userId) {
        return success(userApiService.updateAvatar(userId, request));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并更新当前用户头像")
    public ResponseEntity<AjaxResult> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) String lang,
            HttpServletRequest request) {
        SupportedLocale locale = localeService.resolve(lang, request);
        try {
            ImageUploadValidator.validate(file);
            String fileName = FileUploadUtils.upload(
                    OrderConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
            AvatarDto avatar = new AvatarDto();
            avatar.setAvatar(fileName);
            userApiService.updateAvatar(userId, avatar);
            AjaxResult result = AjaxResult.success(
                    messageService.message(200, locale, "Success"),
                    new ConfigApiDtos.AvatarUploadResponse(
                            fileName, serverConfig.getUrl() + fileName, fileName));
            return ResponseEntity.ok().headers(localeService.responseHeaders(locale)).body(result);
        } catch (ImageUploadValidator.InvalidImageUploadException exception) {
            throw new com.order.api.service.UserApiException(703, "Upload failed");
        } catch (com.order.api.service.UserApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error(
                    "event=avatar_upload_failed userId={} originalFilename={} contentType={} size={}",
                    userId,
                    file == null ? null : file.getOriginalFilename(),
                    file == null ? null : file.getContentType(),
                    file == null ? null : file.getSize(),
                    exception);
            throw new com.order.api.service.UserApiException(703, "Upload failed");
        }
    }

    @PostMapping("/editPassword")
    @Operation(summary = "修改登录密码")
    public AjaxResult editPassword(
            @Valid @RequestBody EditPasswordDto request,
            @RequestAttribute("userId") Long userId) {
        userApiService.editPassword(userId, request);
        return success();
    }

    @PostMapping("/editTradePassword")
    @Operation(summary = "修改交易密码")
    public AjaxResult editTradePassword(
            @Valid @RequestBody EditTradePasswordDto request,
            @RequestAttribute("userId") Long userId) {
        userApiService.editTradePassword(userId, request);
        return success();
    }

    @PostMapping("/checkTradePassword")
    @Operation(summary = "验证交易密码")
    public AjaxResult checkTradePassword(
            @Valid @RequestBody CheckTradePassword request,
            @RequestAttribute("userId") Long userId) {
        userApiService.checkTradePassword(userId, request);
        return success();
    }
}
