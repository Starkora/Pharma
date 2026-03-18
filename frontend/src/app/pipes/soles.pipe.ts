import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'soles',
  standalone: true
})
export class SolesPipe implements PipeTransform {
  transform(value: number | string | null | undefined, decimales: number = 2): string {
    if (value === null || value === undefined || value === '') {
      return 'S/ 0.00';
    }
    
    const numero = typeof value === 'string' ? parseFloat(value) : value;
    
    if (isNaN(numero)) {
      return 'S/ 0.00';
    }
    
    return 'S/ ' + numero.toFixed(decimales).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }
}
