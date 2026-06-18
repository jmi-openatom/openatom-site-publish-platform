package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.entity.Site;
import cn.jmi.openatom.sitepublish.service.PublishedSiteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

@RestController
@RequiredArgsConstructor
public class PublishedSiteController {

    private final PublishedSiteService publishedSiteService;

    @GetMapping({"/published/{slug}", "/published/{slug}/", "/published/{slug}/**"})
    public ResponseEntity<Resource> published(@PathVariable String slug, HttpServletRequest request) {
        Site site = publishedSiteService.findBySlug(slug);
        String matchedPath = String.valueOf(
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE));
        String prefix = "/published/" + slug;
        String relative = matchedPath.length() <= prefix.length()
                ? "index.html"
                : matchedPath.substring(prefix.length()).replaceFirst("^/", "");
        return publishedSiteService.serve(site, relative);
    }

    @GetMapping({"/", "/**"})
    public ResponseEntity<Resource> hosted(HttpServletRequest request) {
        Site site = publishedSiteService.findByHost(request.getHeader("Host"));
        return publishedSiteService.serve(site, request.getRequestURI());
    }
}
