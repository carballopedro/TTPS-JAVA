import { Component, ChangeDetectionStrategy } from '@angular/core';
//import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-acerca',
  templateUrl: './acerca.html',
  standalone: true,
  //imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './acerca.css',
})
export class AcercaComponent {
  // Este componente es sólo informativo así que no necesito lógica extra.
}
