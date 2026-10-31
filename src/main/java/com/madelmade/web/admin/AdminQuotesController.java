package com.madelmade.web.admin;

import com.madelmade.domain.Testimonial;
import com.madelmade.repo.TestimonialRepository;
import com.madelmade.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin › Website › Customer quotes: the quotes shown below the menu on the home page. */
@Controller
@RequestMapping("/admin/quotes")
public class AdminQuotesController {
    private final TestimonialRepository quotes;

    public AdminQuotesController(TestimonialRepository quotes) {
        this.quotes = quotes;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("quotes", quotes.findAllByOrderByDisplayOrderAscIdAsc());
        return "admin/quotes";
    }

    @PostMapping("/new")
    @Transactional
    public String add(@RequestParam String quote, @RequestParam(required = false) String name,
                      @RequestParam(required = false) String detail, RedirectAttributes flash) {
        if (quote == null || quote.isBlank()) {
            flash.addFlashAttribute("errors", List.of("Type the customer's words first."));
            return "redirect:/admin/quotes#add-quote";
        }
        Testimonial t = new Testimonial();
        fill(t, quote, name, detail);
        t.setDisplayOrder((int) quotes.count());
        quotes.save(t);
        flash.addFlashAttribute("ok", "Quote added. It shows on the home page now.");
        return "redirect:/admin/quotes#quote-" + t.getId();
    }

    @PostMapping("/{id}")
    @Transactional
    public String update(@PathVariable Long id, @RequestParam(required = false) String quote, @RequestParam(required = false) String name,
                         @RequestParam(required = false) String detail, @RequestParam(required = false) String move,
                         RedirectAttributes flash) {
        Testimonial t = quotes.findById(id).orElseThrow(NotFoundException::new);
        if (quote != null && quote.isBlank()) {
            flash.addFlashAttribute("errors", List.of("A quote can't be empty, so the old words were kept. Use Delete to remove it."));
            quote = t.getQuote();
        }
        fill(t, quote == null ? t.getQuote() : quote, name, detail);
        if (move != null) Ordering.move(quotes.findAllByOrderByDisplayOrderAscIdAsc(), t, move, Testimonial::setDisplayOrder);
        else if (!flash.getFlashAttributes().containsKey("errors")) flash.addFlashAttribute("ok", "Saved.");
        quotes.save(t);
        return "redirect:/admin/quotes#quote-" + t.getId();
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        quotes.delete(quotes.findById(id).orElseThrow(NotFoundException::new));
        flash.addFlashAttribute("ok", "Quote deleted.");
        return "redirect:/admin/quotes";
    }

    private static void fill(Testimonial t, String quote, String name, String detail) {
        t.setQuote(clip(quote, 600));
        t.setName(blankToNull(clip(name, 80)));
        t.setDetail(blankToNull(clip(detail, 120)));
    }

    private static String clip(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
