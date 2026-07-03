package io.ddys.halo.ddysopen.finder;

import java.util.List;
import java.util.Map;

public interface DdysFinder {
    List<Map<String, Object>> movies(String type, String genre, String region, Integer page, Integer perPage);

    List<Map<String, Object>> latest(Integer limit);

    List<Map<String, Object>> hot(Integer limit);

    List<Map<String, Object>> search(String keyword, Integer limit);

    List<Map<String, Object>> suggest(String keyword);

    List<Map<String, Object>> calendar(Integer year, Integer month);

    List<Map<String, Object>> collections(Integer limit);

    Map<String, Object> collection(String slug);

    Map<String, Object> movie(String slug);

    List<Map<String, Object>> sources(String slug);

    List<Map<String, Object>> related(String slug);

    List<Map<String, Object>> comments(String slug, Integer page, Integer perPage);

    List<Map<String, Object>> shares(Integer page, Integer perPage);

    Map<String, Object> share(String id);

    List<Map<String, Object>> requests(Integer page, Integer perPage);

    List<Map<String, Object>> activities(String type, Integer page, Integer perPage);

    Map<String, Object> user(String username);

    List<Map<String, Object>> types();

    List<Map<String, Object>> genres();

    List<Map<String, Object>> regions();
}
