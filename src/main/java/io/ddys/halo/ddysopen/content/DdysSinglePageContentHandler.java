package io.ddys.halo.ddysopen.content;

import io.ddys.halo.ddysopen.config.DdysSettingsService;
import io.ddys.halo.ddysopen.render.ShortcodeService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactiveSinglePageContentHandler;

@Component
public class DdysSinglePageContentHandler implements ReactiveSinglePageContentHandler {

    private final DdysSettingsService settingsService;
    private final ShortcodeService shortcodeService;

    public DdysSinglePageContentHandler(DdysSettingsService settingsService, ShortcodeService shortcodeService) {
        this.settingsService = settingsService;
        this.shortcodeService = shortcodeService;
    }

    @Override
    public Mono<SinglePageContentContext> handle(@NonNull SinglePageContentContext singlePageContent) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.shortcodesEnabled() || singlePageContent.getContent() == null) {
                    return Mono.just(singlePageContent);
                }
                return shortcodeService.renderContent(singlePageContent.getContent())
                    .map(rendered -> {
                        singlePageContent.setContent(rendered);
                        return singlePageContent;
                    });
            })
            .onErrorReturn(singlePageContent);
    }
}

