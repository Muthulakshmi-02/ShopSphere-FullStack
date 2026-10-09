package com.example.MyProject.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once at startup: fills in Product.effectivePrice for products that existed before the
 * column was added. Safe to leave in place; with nothing to fill it does nothing.
 * (Rows with a null effectivePrice would otherwise vanish from the storefront, because the
 * price filter cannot match null.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EffectivePriceBackfill implements ApplicationRunner {

    private final ProductService productService;

    @Override
    public void run(ApplicationArguments args) {
        int updated = productService.backfillEffectivePrices();
        if (updated > 0) {
            log.info("Backfilled effective price for {} product(s)", updated);
        }
    }
}