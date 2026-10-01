import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CategoryService } from '../../Services/category-service';
import { CategoryResponse } from '../../Models/Category.model';
import { TitleCasePipe } from '@angular/common';
import { ProductService } from '../../Services/product-service';
import { ProductResponse } from '../../Models/Product.model';
@Component({
  selector: 'app-categories-page',
  standalone: true,
  imports: [CommonModule, TitleCasePipe],
  templateUrl: './categories-page.html',
  styleUrls: ['./categories-page.css']
})
export class CategoriesPage implements OnInit {

  private categoryService = inject(CategoryService);
  private router = inject(Router);
  private productService = inject(ProductService);
  categories = signal<CategoryResponse[]>([]);
  products = signal<ProductResponse[]>([]);

  categoryCounts = signal<Record<number, number>>({});

  ngOnInit(): void {

    this.categoryService.getAllCategories().subscribe({
      next: (res) => {
        this.categories.set(res.data || []);
      }
    });

    this.productService
      .getFilteredProducts(
        '',
        null,
        0,
        1000000,
        0,
        1000,
        'name,asc'
      )
      .subscribe({

        next: (res) => {

          const content =
            res.data?.content || [];

          this.products.set(content);

          const counts: Record<number, number> = {};

          content.forEach(product => {

            counts[product.categoryId] =
              (counts[product.categoryId] || 0) + 1;

          });

          this.categoryCounts.set(counts);

        }

      });

  }
  getProductCount(categoryId: number): number {

    return this.categoryCounts()[categoryId] || 0;

  }
  getCategoryIcon(name: string): string {

    switch (name.toLowerCase()) {

      case 'electronics':
        return '💻';

      case 'beauty':
        return '💄';

      case 'toys':
        return '🧸';

      case 'books':
        return '📚';

      case 'home appliances':
        return '🏠';

      default:
        return '📦';
    }
  }
  openCategory(categoryId: number) {

    this.router.navigate(
      ['/products'],
      {
        queryParams: {
          category: categoryId
        }
      }
    );

  }

}