package com.madelmade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.madelmade.domain.HeroSlide;
import com.madelmade.domain.HeroVideo;
import com.madelmade.domain.OptionChoice;
import com.madelmade.domain.Product;
import com.madelmade.repo.ProductRepository;
import com.madelmade.service.HeroService;
import com.madelmade.service.PricingService;
import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs against a fresh throwaway Postgres, seeded with the starting menu. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteTest {
    static final EmbeddedPostgres PG = start();

    static EmbeddedPostgres start() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't start the test database", e);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired PricingService pricing;
    @Autowired HeroService hero;

    @Test
    void homeShowsTheMenu() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Stencil Loaf")))
                .andExpect(content().string(Matchers.containsString("Cookie Butter Latte")));
    }

    @Test
    void homeHeroStartsWithBothLoaves() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("data-hero-slide")))
                .andExpect(content().string(Matchers.containsString("diagonal leaf score")))
                .andExpect(content().string(Matchers.containsString("stenciled with MadelMade")));
    }

    @Test
    @Transactional
    void heroVideoServesPartsForSafari() throws Exception {
        HeroSlide slide = new HeroSlide();
        byte[] mp4 = new byte[2000];
        System.arraycopy("\0\0\0\u0018ftypmp42".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), 0, mp4, 0, 12);
        HeroVideo v = hero.saveVideo(slide, mp4, "application/octet-stream");
        assertThat(v.getContentType()).isEqualTo("video/mp4");
        mvc.perform(get("/hero/video/" + v.getId()).header("Range", "bytes=0-99"))
                .andExpect(status().isPartialContent())
                .andExpect(content().contentType("video/mp4"));
    }

    @Test
    void productPageRenders() throws Exception {
        mvc.perform(get("/p/stencil-loaf")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Add a birth certificate")));
    }

    @Test
    void dashboardNeedsSignIn() throws Exception {
        mvc.perform(get("/admin/orders")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signin?next=%2Fadmin%2Forders"));
    }

    @Test
    void unknownOrderIsNotFound() throws Exception {
        mvc.perform(get("/o/not-a-real-token")).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void stencilWithLettersFlavorAndCertificateIsPricedRight() {
        Product stencil = products.findBySlug("stencil-loaf").orElseThrow();
        List<Long> ids = stencil.getAllOptionGroups().stream()
                .flatMap(g -> g.getActiveChoices().stream())
                .filter(c -> List.of("Whole", "Rosemary garlic", "Text").contains(c.getName()))
                .map(OptionChoice::getId).toList();
        PricingService.Quote q = pricing.quote(stencil,
                new PricingService.Selection(ids, "HAPPY 40", "Script", null, true, "Hazel", null));
        assertThat(q.errors()).isEmpty();
        assertThat(q.unitPrice()).isEqualByComparingTo(new BigDecimal("16"));
    }

    @Test
    @Transactional
    void halfLoafHalvesThePriceAndLettersHaveALimit() {
        Product classic = products.findBySlug("classic-loaf").orElseThrow();
        List<Long> half = classic.getAllOptionGroups().stream()
                .flatMap(g -> g.getActiveChoices().stream())
                .filter(c -> c.getName().equals("Half") || c.isDefaultChoice() && !c.getName().equals("Whole"))
                .map(OptionChoice::getId).toList();
        PricingService.Quote q = pricing.quote(classic, new PricingService.Selection(half, null, null, null, false, null, null));
        assertThat(q.errors()).isEmpty();
        assertThat(q.unitPrice()).isEqualByComparingTo(new BigDecimal("5"));

        Product stencil = products.findBySlug("stencil-loaf").orElseThrow();
        List<Long> letters = stencil.getAllOptionGroups().stream()
                .flatMap(g -> g.getActiveChoices().stream())
                .filter(c -> c.isDefaultChoice() && !c.getName().equals("Butterfly") || c.getName().equals("Text"))
                .map(OptionChoice::getId).toList();
        PricingService.Quote tooLong = pricing.quote(stencil,
                new PricingService.Selection(letters, "HAPPY BIRTHDAY MIRIAM", "Block letters", null, false, null, null));
        assertThat(tooLong.errors()).anyMatch(e -> e.contains("up to 15"));

        PricingService.Quote noFont = pricing.quote(stencil,
                new PricingService.Selection(letters, "HI", null, null, false, null, null));
        assertThat(noFont.errors()).anyMatch(e -> e.contains("pick a font"));
        PricingService.Quote customFont = pricing.quote(stencil,
                new PricingService.Selection(letters, "HI", "Custom", null, false, null, null));
        assertThat(customFont.errors()).anyMatch(e -> e.contains("Describe your font"));
        PricingService.Quote script = pricing.quote(stencil,
                new PricingService.Selection(letters, "HI", "Script", null, false, null, null));
        assertThat(script.errors()).isEmpty();
        assertThat(script.summary()).contains("Text \"HI\" in Script");
    }
}
