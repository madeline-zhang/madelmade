package com.madelmade.web.admin;

import com.madelmade.domain.HeroSlide;
import com.madelmade.repo.HeroSlideRepository;
import com.madelmade.repo.VideoRepository;
import com.madelmade.service.HeroService;
import com.madelmade.service.SettingsService;
import com.madelmade.service.UserFacingException;
import com.madelmade.web.NotFoundException;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin › Home page: the hero's photos and videos, their order, and how long each shows. */
@Controller
@RequestMapping("/admin/hero")
public class AdminHeroController {
    private final HeroSlideRepository slides;
    private final VideoRepository videos;
    private final HeroService hero;
    private final SettingsService settings;

    public AdminHeroController(HeroSlideRepository slides, VideoRepository videos, HeroService hero, SettingsService settings) {
        this.slides = slides;
        this.videos = videos;
        this.hero = hero;
        this.settings = settings;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("slides", slides.findAllByOrderByDisplayOrderAscIdAsc());
        model.addAttribute("hero", hero);
        return "admin/hero";
    }

    @PostMapping("/timing")
    public String timing(@RequestParam String seconds, @RequestParam String fade, RedirectAttributes flash) {
        Double s = number(seconds), f = number(fade);
        if (s == null || s < 3 || s > 60) {
            flash.addFlashAttribute("errors", java.util.List.of("Time on each should be between 3 and 60 seconds."));
        } else if (f == null || f < 0 || f > 5) {
            flash.addFlashAttribute("errors", java.util.List.of("The fade should be between 0 and 5 seconds."));
        } else {
            settings.putAll(Map.of("hero.seconds", String.valueOf(Math.round(s)), "hero.fade", plain(f)));
            flash.addFlashAttribute("ok", "Saved.");
        }
        return "redirect:/admin/hero";
    }

    @PostMapping("/{id}")
    @Transactional
    public String update(@PathVariable Long id, @RequestParam(required = false) String alt,
                         @RequestParam(required = false) String move, RedirectAttributes flash) {
        HeroSlide s = slides.findById(id).orElseThrow(NotFoundException::new);
        s.setAlt(alt == null || alt.isBlank() ? null : alt.trim());
        if (move != null) Ordering.move(slides.findAllByOrderByDisplayOrderAscIdAsc(), s, move, HeroSlide::setDisplayOrder);
        else flash.addFlashAttribute("ok", "Saved.");
        slides.save(s);
        return "redirect:/admin/hero#slide-" + s.getId();
    }

    /** A new video slide, or a new video for an existing one. */
    @PostMapping("/video")
    @Transactional
    public String video(@RequestParam(required = false) Long slide, @RequestParam MultipartFile file,
                       @RequestParam(required = false) String alt, RedirectAttributes flash) throws IOException {
        HeroSlide s = slide == null ? new HeroSlide() : slides.findById(slide).orElseThrow(NotFoundException::new);
        try {
            hero.saveVideo(s, file.getBytes(), file.getContentType());
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
            return "redirect:/admin/hero";
        }
        if (slide == null) {
            s.setDisplayOrder((int) slides.count());
            s.setAlt(alt == null || alt.isBlank() ? "Video of a MadelMade loaf" : alt.trim());
        }
        slides.save(s);
        flash.addFlashAttribute("ok", "Video saved.");
        return "redirect:/admin/hero#slide-" + s.getId();
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        HeroSlide s = slides.findById(id).orElseThrow(NotFoundException::new);
        Long videoId = s.getVideoId();
        slides.delete(s);
        if (videoId != null) videos.deleteById(videoId);
        flash.addFlashAttribute("ok", "Removed from the home page.");
        return "redirect:/admin/hero";
    }

    private static Double number(String s) {
        try {
            return Double.parseDouble(s.trim().replaceAll("(?i)\\s*s(ec(ond)?s?)?$", ""));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String plain(double d) {
        return java.math.BigDecimal.valueOf(Math.round(d * 10) / 10.0).stripTrailingZeros().toPlainString();
    }
}
