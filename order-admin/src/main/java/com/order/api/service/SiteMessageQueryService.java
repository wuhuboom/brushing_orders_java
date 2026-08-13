package com.order.api.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.order.api.controller.dto.ConfigApiDtos;
import com.order.common.i18n.ITranslationsService;
import com.order.common.i18n.SupportedLocale;
import com.order.common.i18n.TranslationResolver;
import com.order.common.i18n.Translations;
import com.order.member.domain.OrderSiteMessage;
import com.order.member.mapper.OrderSiteMessageMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class SiteMessageQueryService {
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderSiteMessageMapper messageMapper;
    private final ITranslationsService translationsService;
    private final ObjectMapper objectMapper;

    public SiteMessageQueryService(
            OrderSiteMessageMapper messageMapper,
            ITranslationsService translationsService,
            ObjectMapper objectMapper) {
        this.messageMapper = messageMapper;
        this.translationsService = translationsService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ConfigApiDtos.PagedResult<ConfigApiDtos.NoticeSummaryResponse> list(
            Long userId,
            SupportedLocale locale,
            int pageNum,
            int pageSize) {
        validatePage(pageNum, pageSize);
        PageHelper.startPage(pageNum, pageSize);
        List<OrderSiteMessage> rows = messageMapper.selectEnabledByMemberId(userId);
        long total = new PageInfo<>(rows).getTotal();
        List<ConfigApiDtos.NoticeSummaryResponse> result = rows.stream()
                .map(message -> new ConfigApiDtos.NoticeSummaryResponse(
                        message.getId(),
                        localized(message, locale).title(),
                        message.getCreateTime()))
                .toList();
        return new ConfigApiDtos.PagedResult<>(result, total);
    }

    @Transactional(readOnly = true)
    public ConfigApiDtos.NoticeDetailResponse get(
            Long userId,
            Long id,
            SupportedLocale locale) {
        OrderSiteMessage message = messageMapper.selectEnabledByIdAndMemberId(id, userId);
        if (message == null) {
            throw new ConfigApiException(404, HttpStatus.NOT_FOUND, "Message not found");
        }
        LocalizedMessage localized = localized(message, locale);
        return new ConfigApiDtos.NoticeDetailResponse(
                message.getId(),
                localized.title(),
                localized.content(),
                message.getCreateTime());
    }

    private LocalizedMessage localized(OrderSiteMessage message, SupportedLocale locale) {
        Translations translations = message.getTranslationsId() == null
                ? null : translationsService.selectTranslationsById(message.getTranslationsId());
        String title = null;
        String content = null;
        for (String candidate : TranslationResolver.candidates(translations, locale, null)) {
            if (!TranslationResolver.hasText(candidate)) {
                continue;
            }
            try {
                JsonNode json = objectMapper.readTree(candidate);
                if (title == null && TranslationResolver.hasText(json.path("title").asText(""))) {
                    title = json.path("title").asText();
                }
                if (content == null && TranslationResolver.hasText(json.path("content").asText(""))) {
                    content = json.path("content").asText();
                }
            } catch (Exception ignored) {
                if (content == null) {
                    content = candidate;
                }
            }
        }
        return new LocalizedMessage(
                title == null ? message.getTitle() : title,
                content == null ? message.getContent() : content);
    }

    private void validatePage(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ConfigApiException(400, HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
    }

    private record LocalizedMessage(String title, String content) {
    }
}
