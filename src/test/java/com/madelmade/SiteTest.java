package com.madelmade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.madelmade.domain.HeroSlide;
import com.madelmade.domain.Video;
import com.madelmade.domain.OptionChoice;
import com.madelmade.domain.ProcessStep;
import com.madelmade.domain.Product;
import com.madelmade.repo.ProcessStepRepository;
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
    @Autowired ProcessStepRepository steps;

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
        Video v = hero.saveVideo(slide, mp4, "application/octet-stream");
        assertThat(v.getContentType()).isEqualTo("video/mp4");
        mvc.perform(get("/videos/" + v.getId()).header("Range", "bytes=0-99"))
                .andExpect(status().isPartialContent())
                .andExpect(content().contentType("video/mp4"));
    }

    @Test
    @Transactional
    void howItsMadeStaysHiddenUntilItHasAStep() throws Exception {
        steps.deleteAll();
        mvc.perform(get("/how-its-made")).andExpect(status().isNotFound());
        mvc.perform(get("/gallery")).andExpect(content().string(Matchers.not(Matchers.containsString("/how-its-made"))));
        ProcessStep s = new ProcessStep();
        s.setTitle("Feed the starter");
        s.setBody("Fresh flour and water.\n\nIt doubles by morning.");
        steps.save(s);
        mvc.perform(get("/how-its-made")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Feed the starter")))
                .andExpect(content().string(Matchers.containsString("<p>It doubles by morning.</p>")));
        mvc.perform(get("/gallery")).andExpect(content().string(Matchers.containsString("/how-its-made")));
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
    void adminLandsOnTheDashboardWithTheCalendar() throws Exception {
        var admin = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("madeline@example.com").roles("ADMIN");
        mvc.perform(get("/admin").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Sales by month")))
                .andExpect(content().string(Matchers.containsString("Bake days")));
        mvc.perform(get("/admin").param("period", "month").param("way", "delivery").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Orders to review")))
                .andExpect(content().string(Matchers.containsString("Loaves to hand off")));
        mvc.perform(get("/admin").param("period", "nonsense").param("way", "nonsense").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/calendar").param("month", "2026-11").with(admin))
                .andExpect(redirectedUrl("/admin?month=2026-11#calendar"));
        mvc.perform(get("/admin/process").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/hero").with(admin)).andExpect(status().isOk())
                .andExpect(content().string(Matchers.not(Matchers.containsString("Phone crop</a>"))));
    }

    @Test
    void privacyPageIsPublic() throws Exception {
        mvc.perform(get("/privacy")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("never sees card or bank details")));
    }

    @Test
    void aboutAnswersWhetherWeShipOutOfTennessee() throws Exception {
        mvc.perform(get("/about")).andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Can you ship my bread?")))
                .andExpect(content().string(Matchers.containsString("I can only ship within Tennessee.")))
                .andExpect(content().string(Matchers.containsString("Can I get a refund?")));
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
