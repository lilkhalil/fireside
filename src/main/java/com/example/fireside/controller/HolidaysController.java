package com.example.fireside.controller;

import com.example.fireside.entity.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;

@Controller
public class HolidaysController {

    private final HashMap<String, String> result = new HashMap<>();
    private String DATE = "13";
    private String MONTH = "5";
    private String STATE = "Праздники России";
    private String URL = "https://www.calend.ru/holidays/" + MONTH + "-" + DATE;

    public HashMap<String, String> getResult() {
        return this.result;
    }

    public void parseHolidays() throws IOException {
        Document document = Jsoup.connect(URL)
                .userAgent("Google Chrome")
                .timeout(5000)
                .referrer("https://google.com")
                .get();
        Elements holidays = document.select("body > div.wrapper > div.block.main" +
                " > div.block.content > div.block.datesList > div.block.holidays > ul.itemsNet > li.three-three" +
                " > div.caption");
        if (holidays.isEmpty()) {
            throw new IOException("Ошибка! Проверьте правильность введенных вами данных.");
        }
        holidays = sortByState(holidays);
        if (holidays.isEmpty()) {
            throw new IOException(STATE + " в этот день не отмечаются!");
        }
        for (int i = 0; i < holidays.size(); i++) {
            Document eachPage = Jsoup.connect(holidays.select(" > span.title > a")
                            .eachAttr("href").get(i))
                    .userAgent("Google Chrome")
                    .timeout(5000)
                    .referrer("https://google.com")
                    .get();
            Elements about = eachPage.select("body > div.wrapper > div.block.main" +
                    " > div.block.content > div.maintext > p");
            StringBuilder description = new StringBuilder();
            if (about.hasText()) {
                for (Element e : about) {
                    description.append(e.text());
                }
            } else {
                description = new StringBuilder("Невозможно найти описание для данного праздника!");
            }
            result.put(holidays.select(" > span.title > a").eachText().get(i), description.toString());
        }
    }

    public Elements sortByState(Elements holidays) {
        holidays.removeIf(e -> !e.select(" > div.link > a").text().equals(STATE));
        return holidays;
    }

    @GetMapping("/holidays")
    public String getHolidays(@AuthenticationPrincipal User user, Model model) throws IOException {
        if (!result.isEmpty()) {
            result.clear();
        }
        try {
            parseHolidays();
        } catch (IOException e) {
            model.addAttribute("exception", e.getMessage());
        }
        DATE = String.valueOf(LocalDateTime.now().getDayOfMonth());
        MONTH = String.valueOf(LocalDateTime.now().getMonthValue());
        URL = "https://www.calend.ru/holidays/" + MONTH + "-" + DATE;
        model.addAttribute("holidays", result);
        model.addAttribute("user", user);
        return "osnova";
    }

    @PostMapping("/holidays")
    public String getDayAndMonth(@RequestParam String date, @RequestParam String month, @RequestParam String state, Model model) {
        DATE = date;
        MONTH = month;
        STATE = state;
        this.URL = "https://www.calend.ru/holidays/" + MONTH + "-" + DATE;
        return "redirect:/holidays";
    }
}