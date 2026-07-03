package io.ddys.halo.ddysopen.content;

import io.ddys.halo.ddysopen.config.DdysSettingsService;
import io.ddys.halo.ddysopen.render.ShortcodeService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.theme.ReactivePostContentHandler;

@Component
public class DdysPostContentHandler implements ReactivePostContentHandler {

    private final DdysSettingsService settingsService;
    private final ShortcodeService shortcodeService;

    public DdysPostContentHandler(DdysSettingsService settingsService, ShortcodeService shortcodeService) {
        this.settingsService = settingsService;
        this.shortcodeService = shortcodeService;
    }

    @Override
    public Mono<PostContentContext> handle(@NonNull PostContentContext postContent) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.shortcodesEnabled() || postContent.getContent() == null) {
                    return Mono.just(postContent);
                }
                return shortcodeService.renderContent(postContent.getContent())
                    .map(rendered -> {
                        postContent.setContent(rendered);
                        return postContent;
                    });
            })
            .onErrorReturn(postContent);
    }
}

