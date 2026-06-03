/* ============================================================
 * Camera.ts
 * Maneja el tamano de canvas y el escalado para que el
 * tablero se vea completo sin importar las dimensiones del
 * navegador. Maximiza el tamano de cada celda para que el
 * tablero ocupe la mayor parte del area disponible.
 * ============================================================ */

export class Camera {
  private tileSize = 32;
  private viewportWidth = 640;
  private viewportHeight = 480;

  /**
   * Configura la camara para que el tablero ocupe el mayor
   * espacio posible dentro de {@code maxWidth} x {@code maxHeight}.
   * Calcula el tamano de celda optimo y fuerza aspect ratio 1:1
   * para que el tablero no se deforme.
   */
  configure(boardWidth: number, boardHeight: number, maxWidth: number, maxHeight: number): void {
    const margin = 12;
    const maxW = Math.max(160, maxWidth - margin * 2);
    const maxH = Math.max(160, maxHeight - margin * 2);
    // Tomamos el menor de los dos tamanos para garantizar
    // que el tablero entre completo sin deformarse.
    const sizeW = Math.floor(maxW / boardWidth);
    const sizeH = Math.floor(maxH / boardHeight);
    this.tileSize = Math.max(12, Math.min(sizeW, sizeH));
    this.viewportWidth = boardWidth * this.tileSize;
    this.viewportHeight = boardHeight * this.tileSize;
  }

  getTileSize(): number { return this.tileSize; }
  getViewportWidth(): number { return this.viewportWidth; }
  getViewportHeight(): number { return this.viewportHeight; }

  /** Convierte coordenadas de celda (x, y) a pixeles en el canvas. */
  cellToPixel(x: number, y: number): { px: number; py: number } {
    return { px: x * this.tileSize, py: y * this.tileSize };
  }
}
