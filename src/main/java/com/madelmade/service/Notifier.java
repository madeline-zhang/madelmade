package com.madelmade.service;

import com.madelmade.config.AppProperties;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderMessage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/** Builds every email from one branded frame and hands it to the mail service. */
@Service
public class Notifier {
    private final MailService mail;
    private final ITemplateEngine templates;
    private final SettingsService settings;
    private final AppProperties props;
    private final Money money;
    private final Formats formats;

    public Notifier(MailService mail, ITemplateEngine templates, SettingsService settings, AppProperties props,
                    Money money, Formats formats) {
        this.mail = mail;
        this.templates = templates;
        this.settings = settings;
        this.props = props;
        this.money = money;
        this.formats = formats;
    }

    public String statusUrl(CustomerOrder o) {
        return props.url("/o/" + o.getStatusToken());
    }

    private String render(String template, Map<String, Object> vars) {
        Context ctx = new Context();
        ctx.setVariable("siteUrl", props.baseUrl());
        ctx.setVariable("logoUrl", props.url("/img/madelmade-logo-email.png"));
        ctx.setVariable("contactEmail", "madelmadewithlove@gmail.com");
        ctx.setVariable("money", money);
        ctx.setVariable("fmt", formats);
        vars.forEach(ctx::setVariable);
        return templates.process(template, ctx);
    }

    /** A customer email using Madeline's editable wording, with an optional personal note. */
    public void toCustomer(CustomerOrder o, EmailWording wording, String note) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("order", o);
        vars.put("heading", settings.subject(wording));
        vars.put("intro", settings.intro(wording));
        vars.put("note", note == null ? "" : note);
        vars.put("statusUrl", statusUrl(o));
        vars.put("showSummary", wording != EmailWording.MESSAGE);
        // Madeline's address is only ever shared privately, once she has approved a pickup.
        boolean shareAddress = wording == EmailWording.APPROVED && !o.isDelivery();
        vars.put("pickupAddress", shareAddress ? settings.get("pickup.address") : "");
        String html = render("email/order", vars);
        mail.send(o.getContactEmail(), o.getContactName(), settings.subject(wording) + " (" + o.getNumber() + ")", html);
    }

    public void newRequestToMadeline(CustomerOrder o) {
        if (!settings.flag("notify.newRequest")) return;
        String html = render("email/admin-order", Map.of(
                "order", o,
                "heading", "New request from " + o.getContactName(),
                "intro", o.isOverCapacity()
                        ? "Heads up: this request would put you over your loaf limit."
                        : "A new order request is waiting for you.",
                "note", o.getCustomerNote() == null ? "" : o.getCustomerNote(),
                "reviewUrl", props.url("/admin/orders/" + o.getId())));
        mail.send(settings.notifyEmail(), "Madeline", "New request " + o.getNumber() + " from " + o.getContactName(), html);
    }

    public void customerReplyToMadeline(CustomerOrder o, OrderMessage m) {
        if (!settings.flag("notify.customerReply")) return;
        String html = render("email/admin-order", Map.of(
                "order", o,
                "heading", o.getContactName() + " replied",
                "intro", "They wrote on their order page:",
                "note", m.getBody(),
                "reviewUrl", props.url("/admin/orders/" + o.getId())));
        mail.send(settings.notifyEmail(), "Madeline", "Reply on " + o.getNumber() + " from " + o.getContactName(), html);
    }

    public void customerCancelledToMadeline(CustomerOrder o) {
        String html = render("email/admin-order", Map.of(
                "order", o,
                "heading", o.getContactName() + " cancelled",
                "intro", "This order was cancelled from the customer's order page.",
                "note", "",
                "reviewUrl", props.url("/admin/orders/" + o.getId())));
        mail.send(settings.notifyEmail(), "Madeline", "Cancelled: " + o.getNumber() + " from " + o.getContactName(), html);
    }

    public void signInCode(String email, String code) {
        String html = render("email/code", Map.of("code", code));
        mail.send(email, null, "Your MadelMade sign-in code: " + code, html);
    }

    public void morningSummary(LocalDate today, List<CustomerOrder> todays, List<CustomerOrder> tomorrows) {
        String html = render("email/summary", Map.of(
                "today", today, "todays", todays, "tomorrows", tomorrows,
                "dashboardUrl", props.url("/admin")));
        mail.send(settings.notifyEmail(), "Madeline",
                "Today: " + todays.size() + " order(s), tomorrow: " + tomorrows.size(), html);
    }
}
