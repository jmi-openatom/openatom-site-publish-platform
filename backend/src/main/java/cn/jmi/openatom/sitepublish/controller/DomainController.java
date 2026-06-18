package cn.jmi.openatom.sitepublish.controller;

import cn.jmi.openatom.sitepublish.common.ApiResponse;
import cn.jmi.openatom.sitepublish.dto.SiteDtos;
import cn.jmi.openatom.sitepublish.service.DomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import cn.jmi.openatom.sitepublish.service.PublishedSiteService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final PublishedSiteService publishedSiteService;

    @GetMapping("/domains")
    public ApiResponse<List<SiteDtos.DomainView>> list() {
        return ApiResponse.ok(domainService.list());
    }

    @PostMapping("/sites/{siteId}/domains")
    public ApiResponse<SiteDtos.DomainView> bind(
            @PathVariable Long siteId,
            @Valid @RequestBody SiteDtos.DomainRequest request
    ) {
        return ApiResponse.ok(domainService.bind(siteId, request), "自定义域名已保存");
    }

    @PostMapping("/domains/{id}/verify")
    public ApiResponse<SiteDtos.DomainView> verify(@PathVariable Long id) {
        return ApiResponse.ok(domainService.verify(id), "域名解析验证通过，SSL 证书已进入自动申请队列");
    }

    @PostMapping("/domains/{id}/ssl")
    public ApiResponse<SiteDtos.DomainView> requestCertificate(@PathVariable Long id) {
        return ApiResponse.ok(domainService.requestCertificate(id), "SSL 证书申请已重新提交");
    }

    @GetMapping("/public/domains/allow")
    public ResponseEntity<Void> allowCertificate(@RequestParam String domain) {
        return publishedSiteService.canServeHost(domain)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
