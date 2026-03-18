import { environment } from '../../environments/environment';

export class AppLogger {
  static error(message: string): void {
    if (!environment.production) {
      console.error(message);
    }
  }
}
