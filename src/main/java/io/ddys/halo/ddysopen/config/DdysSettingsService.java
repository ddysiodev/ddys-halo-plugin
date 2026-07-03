package io.ddys.halo.ddysopen.config;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
public class DdysSettingsService {

    private final ReactiveSettingFetcher settingFetcher;

    public DdysSettingsService(ReactiveSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    public Mono<DdysSettings> fetch() {
        return Mono.zip(
                fetchGroup("basic", BasicSettings.class, new BasicSettings()),
                fetchGroup("cache", CacheSettings.class, new CacheSettings()),
                fetchGroup("display", DisplaySettings.class, new DisplaySettings()),
                fetchGroup("features", FeatureSettings.class, new FeatureSettings()),
                fetchGroup("auth", AuthSettings.class, new AuthSettings())
            )
            .map(tuple -> new DdysSettings(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4(),
                tuple.getT5()
            ))
            .onErrorReturn(DdysSettings.defaults());
    }

    private <T> Mono<T> fetchGroup(String group, Class<T> type, T fallback) {
        return settingFetcher.fetch(group, type)
            .defaultIfEmpty(fallback)
            .onErrorReturn(fallback);
    }
}

