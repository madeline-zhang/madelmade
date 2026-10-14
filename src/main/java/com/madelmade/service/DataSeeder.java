package com.madelmade.service;

import com.madelmade.domain.*;
import com.madelmade.repo.CategoryRepository;
import com.madelmade.repo.GalleryPhotoRepository;
import com.madelmade.repo.ProductRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the starting menu and gallery the first time the site runs on an empty database.
 * After that, everything is edited from the dashboard.
 */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SEED = "static/img/seed/";

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final GalleryPhotoRepository gallery;
    private final PhotoService photos;

    public DataSeeder(CategoryRepository categories, ProductRepository products, GalleryPhotoRepository gallery,
                      PhotoService photos) {
        this.categories = categories;
        this.products = products;
        this.gallery = gallery;
        this.photos = photos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categories.count() > 0) return;
        log.info("Empty database: loading the starting menu");

        Category bread = category("Bread", "bread", 0);
        Category coffee = category("Coffee", "coffee", 1);

        Product classic = product(bread, "Classic Loaf", "classic-loaf", 10, "miriam.jpg", 0,
                "Build your own: size, flavor and score.",
                "Our everyday sourdough, about 2 lb whole. Pick a flavor and a score, and Madeline will confirm your request.");
        sizes(classic);
        flavors(classic);
        OptionGroup score = group(classic, "Score style", OptionGroup.STYLE_SWATCHES, 2);
        swatch(score, "Simple score", "jonah.jpg", "[Placeholder] One clean slash for a classic rustic ear.", 0, false);
        swatch(score, "Double leaf", "miriam.jpg", "[Placeholder] Two wheat leaves cut along a central ear.", 0, false).setDefaultChoice(true);
        swatch(score, "Circular", "rebecca.jpg", "[Placeholder] A ring of cuts that opens like a crown.", 0, false);
        swatch(score, "Pumpkin", "jordan.jpg", "[Placeholder] Ribbed sides that bake into a pumpkin shape.", 0, false);
        swatch(score, "Flower", "rebecca.jpg", "[Placeholder] Petal cuts around the top of the loaf.", 0, false);
        OptionChoice customScore = swatch(score, "Custom", "peter.jpg", "[Placeholder] Describe your idea below; price discussed.", 0, false);
        customScore.setPriceTbd(true);
        products.save(classic);

        Product stencil = product(bread, "Stencil Loaf", "stencil-loaf", 13, "peter.jpg", 1,
                "A flour-stencil design on top.",
                "Sourdough with a flour stencil on top: a butterfly, your own letters, or a custom design. About 2 lb whole.");
        sizes(stencil);
        flavors(stencil);
        OptionGroup design = group(stencil, "Design", OptionGroup.STYLE_SWATCHES, 2);
        swatch(design, "Butterfly", "peter.jpg", "[Placeholder] A butterfly stenciled in flour across the top.", 0, false).setDefaultChoice(true);
        swatch(design, "Letters", "bday.jpg", "[Placeholder] Your words in flour, up to 15 characters.", 0, false).setTextLimit(15);
        OptionChoice customDesign = swatch(design, "Custom", "rebecca.jpg", "[Placeholder] Your own design. Madeline approves it before baking.", 2, false);
        customDesign.setNeedsDesignApproval(true);
        products.save(stencil);

        products.save(product(bread, "Living Bread", "living-bread", 10, "bday.jpg", 2,
                "Whole, plain, \"JESUS\" text design.",
                "A whole plain sourdough loaf with \"JESUS\" in flour on top."));
        products.save(product(bread, "Surely Goodness", "surely-goodness", 12, "jordan.jpg", 3,
                "Whole, rosemary garlic cheddar, double leaf.",
                "A whole rosemary garlic cheddar sourdough with a double leaf score."));
        Product moms = product(bread, "Mom's Best", "moms-best", 8, "jonah.jpg", 4,
                "Whole, cranberry walnut, simple score.",
                "Cranberry walnut bread with a simple score. This one is not sourdough.");
        moms.setBadge("Not sourdough");
        products.save(moms);

        latte(coffee, "Latte", "latte", 4, 0, "A double shot over milk.", true);
        latte(coffee, "Cookie Butter Latte", "cookie-butter-latte", 5, 1,
                "Cookie butter, honey and cinnamon; cold foam when iced.", false);
        latte(coffee, "Banana Latte", "banana-latte", 5, 2,
                "Homemade banana syrup, honey and cinnamon; banana cold foam when iced.", false);

        String[][] pics = {
                {"miriam.jpg", "Miriam's double leaf"}, {"peter.jpg", "Peter's butterfly"},
                {"bday.jpg", "Happy birthday stencil"}, {"rebecca.jpg", "Rebecca's butterflies"},
                {"jordan.jpg", "Jordan's pumpkin"}, {"jonah.jpg", "Jonah's loaf"}};
        for (int i = 0; i < pics.length; i++) {
            GalleryPhoto g = new GalleryPhoto();
            g.setPhotoId(photos.fromClasspath(SEED + pics[i][0]).getId());
            g.setCaption(pics[i][1]);
            g.setDisplayOrder(i);
            gallery.save(g);
        }
    }

    private Category category(String name, String slug, int order) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        c.setDisplayOrder(order);
        return categories.save(c);
    }

    private Product product(Category c, String name, String slug, int price, String photo, int order,
                            String shortDescription, String description) {
        Product p = new Product();
        p.setCategory(c);
        p.setName(name);
        p.setSlug(slug);
        p.setBasePrice(BigDecimal.valueOf(price));
        p.setDisplayOrder(order);
        p.setShortDescription(shortDescription);
        p.setDescription(description);
        p.setCountsAsLoaf(true);
        p.setAllowsBirthCertificate(true);
        if (photo != null) p.setPhotoId(photos.fromClasspath(SEED + photo).getId());
        return p;
    }

    private void latte(Category coffee, String name, String slug, int price, int order, String description, boolean sweetener) {
        Product p = new Product();
        p.setCategory(coffee);
        p.setName(name);
        p.setSlug(slug);
        p.setBasePrice(BigDecimal.valueOf(price));
        p.setDisplayOrder(order);
        p.setShortDescription("Hot or iced");
        p.setDescription(description + " One size, same price hot or iced.");
        p.setCountsAsLoaf(false);
        p.setAllowsBirthCertificate(false);
        p.setLeadTimeDays(1);
        OptionGroup temp = group(p, "Hot or iced", OptionGroup.STYLE_BUTTONS, 0);
        button(temp, "Hot", 0, false).setDefaultChoice(true);
        button(temp, "Iced", 0, false);
        OptionGroup beans = group(p, "Beans (double shot)", OptionGroup.STYLE_BUTTONS, 1);
        button(beans, "Just Love Supernova", 0, false).setDefaultChoice(true);
        button(beans, "Just Love Decaf", 0, false);
        button(beans, "Lavazza", 0, false);
        if (sweetener) {
            OptionGroup sweet = group(p, "Sweetener", OptionGroup.STYLE_BUTTONS, 2);
            button(sweet, "None", 0, false).setDefaultChoice(true);
            for (String s : new String[] {"Honey", "Sugar", "Brown sugar", "Vanilla", "Molasses"}) button(sweet, s, 0, false);
        }
        OptionGroup milk = group(p, "Milk", OptionGroup.STYLE_BUTTONS, 3);
        button(milk, "Whole", 0, false).setDefaultChoice(true);
        button(milk, "Oat", 0, false);
        button(milk, "Almond", 0, false);
        products.save(p);
    }

    private void sizes(Product p) {
        OptionGroup size = group(p, "Size", OptionGroup.STYLE_BUTTONS, 0);
        button(size, "Half", 0, false).setHalvesPrice(true);
        button(size, "Whole", 0, false).setDefaultChoice(true);
    }

    private void flavors(Product p) {
        OptionGroup flavor = group(p, "Flavor", OptionGroup.STYLE_BUTTONS, 1);
        button(flavor, "Plain", 0, false).setDefaultChoice(true);
        button(flavor, "Rosemary garlic", 2, false);
        button(flavor, "Rosemary garlic asiago", 2, false);
        button(flavor, "Custom", 0, true);
    }

    private OptionGroup group(Product p, String name, String style, int order) {
        OptionGroup g = new OptionGroup();
        g.setProduct(p);
        g.setName(name);
        g.setDisplayStyle(style);
        g.setDisplayOrder(order);
        g.setRequired(true);
        p.getOptionGroups().add(g);
        return g;
    }

    private OptionChoice button(OptionGroup g, String name, int priceChange, boolean tbd) {
        OptionChoice c = new OptionChoice();
        c.setGroup(g);
        c.setName(name);
        c.setPriceChange(BigDecimal.valueOf(priceChange));
        c.setPriceTbd(tbd);
        c.setDisplayOrder(g.getChoices().size());
        g.getChoices().add(c);
        return c;
    }

    private OptionChoice swatch(OptionGroup g, String name, String photo, String description, int priceChange,
                                boolean tbd) {
        OptionChoice c = button(g, name, priceChange, tbd);
        c.setDescription(description);
        c.setPhotoId(photos.fromClasspath(SEED + photo).getId());
        return c;
    }
}
