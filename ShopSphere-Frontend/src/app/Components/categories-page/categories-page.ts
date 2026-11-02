// import { Component, OnInit, inject, signal } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { Router } from '@angular/router';
// import { CategoryService } from '../../Services/category-service';
// import { CategoryResponse } from '../../Models/Category.model';

// @Component({
//   selector: 'app-categories-page',
//   standalone: true,
//   imports: [CommonModule],
//   templateUrl: './categories-page.html',
//   styleUrls: ['./categories-page.css']
// })
// export class CategoriesPage implements OnInit {

//   private categoryService = inject(CategoryService);
//   private router = inject(Router);

//   categories = signal<CategoryResponse[]>([]);
//   loading = signal(true);
//   loadError = signal(false);

//   ngOnInit(): void {
//     this.load();
//   }

//   load(): void {
//     this.loading.set(true);
//     this.loadError.set(false);

//     // The counts now come from the server with each category (productCount). This page used to
//     // download up to 1000 PRODUCTS just to count them per category, which was slow, wrong once
//     // there are more than 1000, and disagreed with the sidebar on the shop page.
//     this.categoryService.getAllCategories().subscribe({
//       next: (res) => {
//         this.categories.set(res.data || []);
//         this.loading.set(false);
//       },
//       error: () => {
//         this.loading.set(false);
//         this.loadError.set(true);
//       }
//     });
//   }

//   productCountLabel(category: CategoryResponse): string {
//     const n = category.productCount ?? 0;
//     return n === 1 ? '1 product' : `${n} products`;
//   }

//   getCategoryIcon(name: string): string {
//     const key = (name || '').toLowerCase().trim();
//     switch (key) {
//       case 'electronics': return '💻';
//       case 'beauty': return '💄';
//       case 'toys': return '🧸';
//       case 'books': return '📚';
//       case 'home appliances': return '🏠';
//       case 'saree': return '👗';
//       default: return '📦';
//     }
//   }

//   openCategory(categoryId: number) {
//     this.router.navigate(['/products'], { queryParams: { category: categoryId } });
//   }
// }
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CategoryService } from '../../Services/category-service';
import { CategoryResponse } from '../../Models/Category.model';

@Component({
  selector: 'app-categories-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './categories-page.html',
  styleUrls: ['./categories-page.css']
})
export class CategoriesPage implements OnInit {

  private categoryService = inject(CategoryService);
  private router = inject(Router);

  categories = signal<CategoryResponse[]>([]);
  loading = signal(true);
  loadError = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);

    // The counts come from the server with each category (productCount).
    this.categoryService.getAllCategories().subscribe({
      next: (res) => {
        this.categories.set(res.data || []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.loadError.set(true);
      }
    });
  }

  isEmpty(category: CategoryResponse): boolean {
    return (category.productCount ?? 0) === 0;
  }

  productCountLabel(category: CategoryResponse): string {
    const n = category.productCount ?? 0;
    return n === 1 ? '1 product' : `${n} products`;
  }

  getCategoryIcon(name: string): string {
    const key = (name || '').toLowerCase().trim();
    switch (key) {
      case 'electronics': return '💻';
      case 'beauty': return '💄';
      case 'toys': return '🧸';
      case 'books': return '📚';
      case 'home appliances': return '🏠';
      case 'saree': return '👗';
      default: return '📦';
    }
  }

  // CHANGED: takes the whole category. A category with no products used to open an empty shop
  // page; now it is shown as "Coming soon" and does nothing when clicked.
  openCategory(category: CategoryResponse): void {
    if (this.isEmpty(category)) return;
    this.router.navigate(['/products'], { queryParams: { category: category.categoryId } });
  }
}