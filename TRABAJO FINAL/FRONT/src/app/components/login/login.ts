import { ChangeDetectorRef, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, NgIf],
  standalone: true,
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  protected isSubmitting = false;
  protected isPasswordVisible = false;
  protected serverError?: string;
  @ViewChild('loginFormElement') private readonly formElement?: ElementRef<HTMLFormElement>;

  // Armo el formulario reactivo con todas las validaciones que necesito.
  protected readonly loginForm = this.fb.nonNullable.group({
    email: [
      '',
      [
        Validators.required,
        Validators.email,
        Validators.pattern(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)
      ]
    ],
    password: ['', Validators.required],
    remember: [false]
  });

  // Manejo el submit manualmente para validar y disparar el servicio.
  // envío el formulario validando primero y mostrando errores del backend
  protected onSubmit() {
    if (this.isSubmitting) {
      return;
    }

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.formElement?.nativeElement.reportValidity();
      return;
    }

    this.isSubmitting = true;
    this.serverError = undefined;
    const { email, password, remember } = this.loginForm.getRawValue(); // saco los valores ya validados

    // me aseguro que antes de un nuevo login se limpien tokens viejos
    this.authService.logout();

    this.authService.login({ username: email, password }, remember).subscribe({
      next: () => {
        this.isSubmitting = false;
        const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');
        const target = redirectTo && redirectTo.startsWith('/') ? redirectTo : '/';
        this.router.navigateByUrl(target);
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.isSubmitting = false;
        const apiMessage =
          typeof error?.error === 'string'
            ? error.error
            : error?.error?.message;
        this.serverError =
          apiMessage ??
          'No pudimos iniciar sesión. Verificá tus datos e intentá nuevamente.';
        this.cdr.markForCheck();
      }
    });
  }

  // Con este método muestro/oculto el campo de contraseña.
  // alterno entre mostrar y ocultar la contraseña
  protected togglePasswordVisibility() {
    this.isPasswordVisible = !this.isPasswordVisible;
  }
}
