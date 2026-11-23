package com.example.MyProject.Configuration;

import com.example.MyProject.Enum.ProductStatus;
import com.example.MyProject.Enum.Role;
import com.example.MyProject.Models.Category;
import com.example.MyProject.Models.Coupon;
import com.example.MyProject.Models.Product;
import com.example.MyProject.Models.User;
import com.example.MyProject.Repository.CategoryRepository;
import com.example.MyProject.Repository.CouponRepository;
import com.example.MyProject.Repository.ProductRepository;
import com.example.MyProject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Automatically initializes seed data on startup if the database is fresh:
 * 1. Default Administrator account (admin@shopsphere.com / Admin@123)
 * 2. Standard product categories
 * 3. Starter e-commerce products with images and pricing
 * 4. Welcome promo coupons (WELCOME10, FLAT100, SAVE20)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@shopsphere.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Override
    public void run(String... args) {
        initAdminUser();
        initCategoriesAndProducts();
        initCoupons();
    }

    private void initAdminUser() {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .userName(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Initialized default Administrator account: {} / {}", adminEmail, adminPassword);
        }
    }

    private void initCategoriesAndProducts() {
        if (categoryRepository.count() > 0) {
            return;
        }

        // 1. Seed Categories
        Category electronics = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Smartphones, headphones, laptops, and smart gadgets")
                .build());

        Category fashion = categoryRepository.save(Category.builder()
                .name("Fashion")
                .description("Men's and women's contemporary clothing and apparel")
                .build());

        Category home = categoryRepository.save(Category.builder()
                .name("Home & Living")
                .description("Modern home decor, lighting, and kitchenware")
                .build());

        Category accessories = categoryRepository.save(Category.builder()
                .name("Accessories")
                .description("Bags, watches, sunglasses, and leather goods")
                .build());

        log.info("Initialized default product categories.");

        // 2. Seed Starter Products
        List<Product> products = List.of(
                Product.builder()
                        .name("Pro Wireless Noise Cancelling Headphones")
                        .description("Premium acoustic architecture, 40-hour battery life, spatial audio, and adaptive active noise cancellation.")
                        .price(3499.0)
                        .effectivePrice(2974.15)
                        .discountPercentage(15)
                        .stock(45)
                        .rating(4.8)
                        .reviewCount(32)
                        .status(ProductStatus.IN_STOCK)
                        .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80")
                        .imageUrls(new ArrayList<>(List.of(
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80",
                                "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800&auto=format&fit=crop&q=80"
                        )))
                        .category(electronics)
                        .build(),

                Product.builder()
                        .name("Minimalist Smart Watch Series 7")
                        .description("AMOLED touch display, continuous heart rate and SpO2 tracking, sleep analysis, and water resistance to 50m.")
                        .price(4999.0)
                        .effectivePrice(4499.10)
                        .discountPercentage(10)
                        .stock(30)
                        .rating(4.6)
                        .reviewCount(18)
                        .status(ProductStatus.IN_STOCK)
                        .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80")
                        .imageUrls(new ArrayList<>(List.of(
                                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80"
                        )))
                        .category(electronics)
                        .build(),

                Product.builder()
                        .name("Classic Organic Cotton Crewneck Tee")
                        .description("Crafted from 100% certified organic combed cotton. Pre-shrunk, breathable, and designed for lasting everyday comfort.")
                        .price(799.0)
                        .effectivePrice(799.0)
                        .discountPercentage(0)
                        .stock(80)
                        .rating(4.7)
                        .reviewCount(42)
                        .status(ProductStatus.IN_STOCK)
                        .imageUrl("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&auto=format&fit=crop&q=80")
                        .imageUrls(new ArrayList<>(List.of(
                                "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&auto=format&fit=crop&q=80"
                        )))
                        .category(fashion)
                        .build(),

                Product.builder()
                        .name("Vintage Artisan Leather Backpack")
                        .description("Handcrafted from full-grain vegetable-tanned leather. Features a 15-inch padded laptop sleeve and antique brass hardware.")
                        .price(2899.0)
                        .effectivePrice(2319.20)
                        .discountPercentage(20)
                        .stock(25)
                        .rating(4.9)
                        .reviewCount(24)
                        .status(ProductStatus.IN_STOCK)
                        .imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80")
                        .imageUrls(new ArrayList<>(List.of(
                                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80"
                        )))
                        .category(accessories)
                        .build()
        );

        productRepository.saveAll(products);
        log.info("Initialized {} default starter products.", products.size());
    }

    private void initCoupons() {
        if (couponRepository.count() > 0) {
            return;
        }

        List<Coupon> coupons = List.of(
                Coupon.builder()
                        .code("WELCOME10")
                        .type("percent")
                        .value(0.10)
                        .minOrderAmount(300.0)
                        .active(true)
                        .expiresAt(LocalDateTime.now().plusMonths(6))
                        .build(),

                Coupon.builder()
                        .code("SAVE20")
                        .type("percent")
                        .value(0.20)
                        .minOrderAmount(1000.0)
                        .active(true)
                        .expiresAt(LocalDateTime.now().plusMonths(6))
                        .build(),

                Coupon.builder()
                        .code("FLAT100")
                        .type("flat")
                        .value(100.0)
                        .minOrderAmount(500.0)
                        .active(true)
                        .expiresAt(LocalDateTime.now().plusMonths(6))
                        .build()
        );

        couponRepository.saveAll(coupons);
        log.info("Initialized default promotional coupons: WELCOME10, SAVE20, FLAT100.");
    }
}
