import { Composition } from "remotion";
import { ShortCut, SHORT_CUT_SECONDS } from "./ShortCut";

export const Root = () => (
  <Composition
    id="ShortCut"
    component={ShortCut}
    durationInFrames={SHORT_CUT_SECONDS * 30}
    fps={30}
    width={1080}
    height={1920}
  />
);
