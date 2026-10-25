package com.madelmade.web.admin;

import com.madelmade.domain.ProcessStep;
import com.madelmade.domain.Video;
import com.madelmade.repo.ProcessStepRepository;
import com.madelmade.repo.VideoRepository;
import com.madelmade.service.SettingsService;
import com.madelmade.service.UserFacingException;
import com.madelmade.service.VideoService;
import com.madelmade.web.NotFoundException;
import java.io.IOException;
import java.util.HashMap;
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

/** Admin › How it's made: the page's intro and its steps, each with a photo or a looping video. */
@Controller
@RequestMapping("/admin/process")
public class AdminProcessController {
    private final ProcessStepRepository steps;
    private final VideoRepository videoRepo;
    private final VideoService videos;
    private final SettingsService settings;

    public AdminProcessController(ProcessStepRepository steps, VideoRepository videoRepo, VideoService videos, SettingsService settings) {
        this.steps = steps;
        this.videoRepo = videoRepo;
        this.videos = videos;
        this.settings = settings;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("steps", steps.findAllByOrderByDisplayOrderAscIdAsc());
        model.addAttribute("videos", videos);
        return "admin/process";
    }

    @PostMapping("/intro")
    public String intro(@RequestParam Map<String, String> params, RedirectAttributes flash) {
        Map<String, String> values = new HashMap<>();
        for (String k : new String[]{"process.title", "process.titleAccent", "process.intro"}) {
            if (params.containsKey(k)) values.put(k, params.get(k));
        }
        settings.putAll(values);
        flash.addFlashAttribute("ok", "Saved.");
        return "redirect:/admin/process";
    }

    @PostMapping("/new")
    @Transactional
    public String add(@RequestParam String title, @RequestParam(required = false) String body, RedirectAttributes flash) {
        if (title == null || title.isBlank()) {
            flash.addFlashAttribute("errors", java.util.List.of("Give the step a title."));
            return "redirect:/admin/process#add-step";
        }
        ProcessStep s = new ProcessStep();
        s.setTitle(trim(title, 120));
        s.setBody(body == null || body.isBlank() ? null : body.trim());
        s.setDisplayOrder((int) steps.count());
        steps.save(s);
        flash.addFlashAttribute("ok", "Step added. Now add its photo or video.");
        return "redirect:/admin/process#step-" + s.getId();
    }

    @PostMapping("/{id}")
    @Transactional
    public String update(@PathVariable Long id, @RequestParam(required = false) String title,
                         @RequestParam(required = false) String body, @RequestParam(required = false) String move,
                         RedirectAttributes flash) {
        ProcessStep s = steps.findById(id).orElseThrow(NotFoundException::new);
        if (title != null && !title.isBlank()) s.setTitle(trim(title, 120));
        if (body != null) s.setBody(body.isBlank() ? null : body.trim());
        if (move != null) Ordering.move(steps.findAllByOrderByDisplayOrderAscIdAsc(), s, move, ProcessStep::setDisplayOrder);
        else if (title != null && title.isBlank()) flash.addFlashAttribute("errors", java.util.List.of("A step needs a title, so the old one was kept."));
        else flash.addFlashAttribute("ok", "Saved.");
        steps.save(s);
        return "redirect:/admin/process#step-" + s.getId();
    }

    /** A video replaces the step's photo, and the other way round (see the photo editor). */
    @PostMapping("/{id}/video")
    @Transactional
    public String video(@PathVariable Long id, @RequestParam MultipartFile file, RedirectAttributes flash) throws IOException {
        ProcessStep s = steps.findById(id).orElseThrow(NotFoundException::new);
        try {
            Video v = videos.save(s.getVideoId(), file.getBytes(), file.getContentType());
            s.setVideoId(v.getId());
            s.setPhotoId(null);
            steps.save(s);
            flash.addFlashAttribute("ok", "Video saved.");
        } catch (UserFacingException e) {
            flash.addFlashAttribute("errors", e.getMessages());
        }
        return "redirect:/admin/process#step-" + s.getId();
    }

    @PostMapping("/{id}/remove-media")
    @Transactional
    public String removeMedia(@PathVariable Long id, RedirectAttributes flash) {
        ProcessStep s = steps.findById(id).orElseThrow(NotFoundException::new);
        Long videoId = s.getVideoId();
        s.setVideoId(null);
        s.setPhotoId(null);
        steps.save(s);
        if (videoId != null) videoRepo.deleteById(videoId);
        flash.addFlashAttribute("ok", "Removed.");
        return "redirect:/admin/process#step-" + s.getId();
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        ProcessStep s = steps.findById(id).orElseThrow(NotFoundException::new);
        Long videoId = s.getVideoId();
        steps.delete(s);
        if (videoId != null) videoRepo.deleteById(videoId);
        flash.addFlashAttribute("ok", "Step deleted.");
        return "redirect:/admin/process";
    }

    private static String trim(String s, int max) {
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
