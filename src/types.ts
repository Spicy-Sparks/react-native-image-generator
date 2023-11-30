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

export interface ITextLayer {
  text: string;
  fontSize?: number;
  fontFamily?: string;
  color: [number, number, number, number];
  width: number;
  height: number;
  x: number;
  y: number;
  maxLines: number;
  bold?: boolean;
}

export type ILayer = IPictureLayer | ITextLayer;

export interface IConfig {
  filePath: string;
  width: number;
  height: number;
  base64?: boolean;
}
