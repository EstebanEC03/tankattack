/* ============================================================
 * Camera.ts
 * Maneja el tamano de canvas y el escalado para que el
 * tablero se vea completo sin importar las dimensiones del
 * navegador.
 * ============================================================ */

export class Camera {
  private tileSize = 32;
  private viewportWidth = 640;
  private viewportHeight = 480;

  configure(boardWidth: number, boardHeight: number, maxWidth: number, maxHeight: number): void {
    const margin = 16;
    const maxW = Math.max(160, maxWidth - margin * 2);
    const maxH = Math.max(160, maxHeight - margin * 2);
    const sizeW = Math.floor(maxW / boardWidth);
    const sizeH = Math.floor(maxH / boardHeight);
    this.tileSize = Math.max(8, Math.min(sizeW, sizeH));
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
