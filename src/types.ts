export interface IPictureLayer {
  uri: string;
  width: number;
  height: number;
  x: number;
  y: number;
  skewX?: number;
  skewY?: number;
  radius?: number;
}

export type Color = [number, number, number];
export type Alignment = 'left' | 'center' | 'right';

export interface ITextLayer {
  text: string;
  fontSize?: number;
  fontFamily?: string;
  color: Color;
  opacity?: number;
  width: number;
  height: number;
  x: number;
  y: number;
  maxLines: number;
  alignment?: Alignment;
  bold?: boolean;
}

export type ILayer = IPictureLayer | ITextLayer;

export interface IConfig {
  filePath: string;
  width: number;
  height: number;
  base64?: boolean;
}
