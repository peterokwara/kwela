// Recovered from Replay <head> dumps (head1/head2/head3).
// These provide richer template variables and print CSS than the partial reconstruction.

export const TEMPLATE_VARS_LAYOUT1 = String.raw`:root {
  --accent-color: #000000;
  --align-items-header: center;
  --block-heading-left-layout: HORIZONTAL;
  --block-heading-right-layout: HORIZONTAL;
  --border-color-header: #000;
  --border-spacing-heading: 8pt;
  --border-spacing-section-title: 3pt;
  --border-width-header-bottom: 1pt;
  --border-width-header-top: 0;
  --border-width-section-title: 1pt;
  --bullet-char: '•';
  --bullet-size: 12pt;
  --bullet-weight: 300;
  --company-layout: HORIZONTAL;
  --date-alignment: LEFT;
  --date-format: NUMBERS;
  --education-by: DEGREE;
  --education-layout: STACKED;
  --font-family: Tinos;
  --font-size-body-copy: 10pt;
  --font-size-full-name: 22pt;
  --font-size-heading-primary: 12pt;
  --font-size-heading-secondary: 10pt;
  --font-size-minor-copy: 10pt;
  --font-size-section-title: 10pt;
  --font-size-separator: 11pt;
  --font-weight-body-copy: 300;
  --font-weight-full-name: 600;
  --font-weight-heading-primary: 600;
  --font-weight-heading-secondary-work: 600;
  --font-weight-heading-secondary: 300;
  --font-weight-minor-copy-work: 600;
  --font-weight-minor-copy: 300;
  --font-weight-section-title: 300;
  --font-weight-separator: 300;
  --header-layout: NORMAL;
  --letter-spacing-heading-primary: 0.0;
  --letter-spacing-heading-secondary: 0.0;
  --letter-spacing-section-title: 0.0;
  --line-height-block-heading: 100%;
  --line-height-list-item: 130%;
  --line-height: 130%;
  --location-alignment: LEFT;
  --padding-x: 0.75in;
  --padding-y: 0.75in;
  --page-size: letter;
  --position-alignment: LEFT;
  --separator-char: '•';
  --separator-spacing: 0 4pt;
  --skills-layout-columns: 3;
  --skills-layout: COMMA;
  --spacing-block-heading-sub-block: 4pt;
  --spacing-block-heading: 4pt;
  --spacing-content-blocks: 4pt;
  --spacing-header-bottom: 12pt;
  --spacing-header-bottom: var(--border-spacing-heading, 0);
  --spacing-header-content: 4pt;
  --spacing-list: 4pt;
  --spacing-section-in-block: 4pt;
  --spacing-section-in-sub-block: 4pt;
  --spacing-section-sub-blocks: 4pt;
  --spacing-sections: 12pt;
  --spacing-titles-content: 4pt;
  --text-align-header: center;
  --text-transform-full-name: none;
  --text-transform-heading-primary: none;
  --text-transform-heading-secondary: none;
  --text-transform-minor-copy: none;
  --text-transform-section-title: uppercase;
  --work-dates-by: COMPANY;
  --work-experience-by: COMPANY;
  --work-locations-by: COMPANY;
  --work-type-alignment: LEFT;
}`;

export const TEMPLATE_VARS_LAYOUT2 = String.raw`:root {
  --accent-color: #000000;
  --align-items-header: flex-start;
  --block-heading-left-layout: HORIZONTAL;
  --block-heading-right-layout: HORIZONTAL;
  --border-spacing-heading: 20pt;
  --border-width-header-bottom: 0;
  --border-width-header-top: 5pt;
  --border-width-section-title: 0;
  --bullet-char: '•';
  --bullet-size: 12pt;
  --bullet-weight: 300;
  --color-full-name: #000;
  --company-layout: HORIZONTAL;
  --date-alignment: LEFT;
  --date-format: NUMBERS;
  --education-by: DEGREE;
  --education-layout: STACKED;
  --font-family: Poppins;
  --font-size-body-copy: 10pt;
  --font-size-full-name: 22pt;
  --font-size-heading-primary: 10pt;
  --font-size-heading-secondary: 10pt;
  --font-size-minor-copy: 10pt;
  --font-size-section-title: 10pt;
  --font-size-separator: 11pt;
  --font-weight-body-copy: 300;
  --font-weight-full-name: 600;
  --font-weight-heading-primary: 600;
  --font-weight-heading-secondary-work: 600;
  --font-weight-heading-secondary: 300;
  --font-weight-minor-copy-work: 600;
  --font-weight-minor-copy: 300;
  --font-weight-section-title: 600;
  --font-weight-separator: 300;
  --header-layout: NORMAL;
  --letter-spacing-heading-primary: 0.0;
  --letter-spacing-heading-secondary: 0.0;
  --letter-spacing-section-title: 0.0;
  --line-height-block-heading: 100%;
  --line-height-list-item: 130%;
  --line-height: 120%;
  --location-alignment: LEFT;
  --padding-x: 0.75in;
  --padding-y: 0.75in;
  --page-size: letter;
  --position-alignment: LEFT;
  --separator-char: '•';
  --separator-spacing: 0 4pt;
  --skills-layout-columns: 3;
  --skills-layout: COMMA;
  --spacing-block-heading-sub-block: 4pt;
  --spacing-block-heading: 4pt;
  --spacing-content-blocks: 12pt;
  --spacing-header-content: 4pt;
  --spacing-header-top: var(--border-spacing-heading, 0);
  --spacing-list: 4pt;
  --spacing-section-in-block: 4pt;
  --spacing-section-in-sub-block: 4pt;
  --spacing-section-sub-blocks: 4pt;
  --spacing-sections: 16pt;
  --spacing-titles-content: 10pt;
  --text-align-header: left;
  --text-transform-full-name: none;
  --text-transform-heading-primary: none;
  --text-transform-heading-secondary: none;
  --text-transform-minor-copy: none;
  --text-transform-section-title: uppercase;
  --work-dates-by: COMPANY;
  --work-experience-by: COMPANY;
  --work-locations-by: COMPANY;
  --work-type-alignment: LEFT;
}`;

export const TEMPLATE_VARS_LAYOUT3 = String.raw`:root {
  --accent-color: #000000;
  --align-items-header: flex-start;
  --block-heading-left-layout: HORIZONTAL;
  --block-heading-right-layout: HORIZONTAL;
  --border-spacing-heading: 10pt;
  --border-width-header-bottom: 0;
  --border-width-header-top: 0;
  --border-width-section-title: 0;
  --bullet-char: '•';
  --bullet-size: 12pt;
  --bullet-weight: 300;
  --company-layout: HORIZONTAL;
  --date-alignment: LEFT;
  --date-format: NUMBERS;
  --education-by: DEGREE;
  --education-layout: STACKED;
  --font-family: Poppins;
  --font-size-body-copy: 11pt;
  --font-size-full-name: 22pt;
  --font-size-heading-primary: 12pt;
  --font-size-heading-secondary: 11pt;
  --font-size-minor-copy: 11pt;
  --font-size-section-title: 12pt;
  --font-size-separator: 11pt;
  --font-weight-body-copy: 300;
  --font-weight-full-name: 600;
  --font-weight-heading-primary: 600;
  --font-weight-heading-secondary-work: 600;
  --font-weight-heading-secondary: 300;
  --font-weight-minor-copy-work: 600;
  --font-weight-minor-copy: 300;
  --font-weight-section-title: 600;
  --font-weight-separator: 300;
  --header-layout: NORMAL;
  --letter-spacing-heading-primary: 0.0;
  --letter-spacing-heading-secondary: 0.0;
  --letter-spacing-section-title: 0.0;
  --line-height-block-heading: 100%;
  --line-height-list-item: 130%;
  --line-height: 120%;
  --location-alignment: LEFT;
  --padding-x: 0.75in;
  --padding-y: 0.75in;
  --page-size: letter;
  --position-alignment: LEFT;
  --separator-char: '•';
  --separator-spacing: 0 2pt;
  --skills-layout-columns: 3;
  --skills-layout: COMMA;
  --spacing-block-heading-sub-block: 4pt;
  --spacing-block-heading: 4pt;
  --spacing-content-blocks: 8pt;
  --spacing-header-content: 4pt;
  --spacing-list: 4pt;
  --spacing-section-in-block: 4pt;
  --spacing-section-in-sub-block: 8pt;
  --spacing-section-sub-blocks: 8pt;
  --spacing-sections: 22pt;
  --spacing-titles-content: 10pt;
  --text-align-header: left;
  --text-transform-full-name: none;
  --text-transform-heading-primary: none;
  --text-transform-heading-secondary: none;
  --text-transform-minor-copy: none;
  --text-transform-section-title: uppercase;
  --work-dates-by: COMPANY;
  --work-experience-by: COMPANY;
  --work-locations-by: COMPANY;
  --work-type-alignment: LEFT;
}`;

export const RESUME_STRUCTURAL_PRINT_CSS = String.raw`:root{--color: var(--color-body, #000);--font-size: var(--font-size-body-copy, 11pt);--font-weight: var(--font-weight-body-copy, 400)}*{margin:0;word-break:break-word}*,*:before,*:after{box-sizing:border-box}body{color:var(--color);font-weight:var(--font-weight);font-size:var(--font-size);font-family:var(--font-family);line-height:var(--line-height);letter-spacing:var(--letter-spacing, 0);-webkit-print-color-adjust:exact;print-color-adjust:exact;-prince-linebreak-magic:none}.page-break-avoid-inside{page-break-inside:avoid}.page-break-avoid-after{page-break-after:avoid}.page-break-avoid-after-sibling+*{page-break-before:avoid}.page-break-before{page-break-before:always}a,a:hover,a:visited,a:focus{color:#000;text-decoration:none}.separated>:not(:last-child):after{padding:var(--separator-char-spacing, var(--separator-spacing));color:var(--color);font-weight:var(--font-weight-separator, var(--font-weight));font-size:var(--font-size-separator, var(--font-size));content:var(--separator-char)}.body:not(.columns)>*+*{margin-top:var(--spacing-sections, 16pt)}.content{margin-left:var(--offset-content, 0)}.content>*+*{margin-top:var(--spacing-sections, 16pt)}.header{display:flex;flex-flow:column nowrap;align-items:var(--align-items-header, flex-start);margin-left:var(--spacing-header-left, 0);padding-top:var(--spacing-header-top, 0)!important;padding-bottom:var(--spacing-header-bottom, 0);border-top:var(--border-width-header-top, 0) solid var(--border-color-header, var(--accent-color));border-bottom:var(--border-width-header-bottom, 0) solid var(--border-color-header, var(--accent-color))}.header-content{display:flex;flex-flow:column nowrap;width:100%;text-align:var(--text-align-header, left)}.header-content>*+*{margin-top:var(--spacing-header-content, 4pt)}.header .contact-details{display:flex;flex-flow:row wrap;align-items:baseline;justify-content:var(--align-items-header, flex-start);font-size:var(--font-size-contact-details, var(--font-size))}.header .contact-details .contact-item{line-height:var(--line-height-contact-details, var(--line-height))}.header .full-name{padding-bottom:var(--spacing-full-name-bottom, 0);color:var(--color-full-name, var(--accent-color));font-weight:var(--font-weight-full-name, var(--font-weight));font-size:var(--font-size-full-name, var(--font-size));line-height:var(--line-height-full-name, var(--line-height));text-transform:var(--text-transform-full-name);word-break:break-word}.header.left{flex:0 0 var(--width-header-left, 33%)}.header.left .header-content>*+*{margin-top:var(--spacing-sections)}.header.split .header-content>*+*{margin-top:0}.header.split .header-content{flex-flow:row nowrap;justify-content:space-between;width:100%;column-gap:var(--spacing-header-content, 5%)}.header.split .header-primary{flex:0 0 var(--width-header-primary, 75%);max-width:var(--width-header-primary, 75%)}.header.split .header-secondary{flex:0 0 calc(100% - var(--width-header-primary, 75%) - var(--spacing-header-content, 5%))}.header.left .contact-details,.header.split .contact-details{display:block}.header.left .contact-details>*+*,.header.split .contact-details>*+*{margin-top:var(--spacing-contact-details, 0)}.header.split .contact-details.separated>:not(:last-child):after{padding:0;content:""}.target-title{color:var(--color-target-title, var(--color));font-weight:var( --font-weight-target-title, var(--font-weight-heading-primary, var(--font-weight)) );font-size:var( --font-size-target-title, var(--font-size-heading-primary, var(--font-size)) );line-height:var(--line-height-target-title, var(--line-height))}.header .target-title{font-size:var( --font-size-target-title, var(--font-size-heading-secondary, var(--font-size)) )}.target-title:not(:first-child){margin-top:var(--spacing-target-title-top, var(--spacing-sections))}.section>*+*{margin-top:var(--spacing-titles-content, 0)}.section-content>*+*{margin-top:var(--spacing-content-blocks, 0)}.section-title{padding-top:var(--padding-top-section-title, 0)!important;padding-bottom:var( --padding-bottom-section-title, var(--border-spacing-section-title, 0) );border-top:var(--border-top-width-section-title, 0) solid #000;border-bottom:var( --border-bottom-width-section-title, var(--border-width-section-title) ) solid #000}.section-title>h6{width:var(--width-section-title-text, 100%);color:var(--accent-color);font-weight:var(--font-weight-section-title, var(--font-weight));font-size:var(--font-size-section-title, var(--font-size));letter-spacing:var(--letter-spacing-section-title);text-transform:var(--text-transform-section-title)}.block>*+*{margin-top:var(--spacing-section-in-block, 3pt)}.block:not(:first-child){padding-top:var(--border-spacing-block, 0);border-top:var(--border-width-block, 0) solid #000}.sub-block>*+*{margin-top:var(--spacing-section-in-sub-block, 3pt)}.sub-block-wrapper:not(:first-child){margin-top:var( --spacing-block-heading-sub-block, var(--spacing-section-sub-blocks, 3pt) )}.sub-block-wrapper>*+*{margin-top:var(--spacing-section-sub-blocks, 3pt)}.block-heading-wrapper>*+*{margin-top:var(--spacing-section-block-heading-blurb)}.block-heading>*+*{margin-top:var(--spacing-block-heading, 2pt)}.block-heading *{line-height:var(--line-height-block-heading, revert)}.block-heading .heading-primary{color:var(--color-heading-primary, var(--color));font-weight:var(--font-weight-heading-primary, var(--font-weight));font-size:var(--font-size-heading-primary, var(--font-size));letter-spacing:var(--letter-spacing-heading-primary, 0);text-transform:var(--text-transform-heading-primary)}.col-aside .block-heading .heading-primary{font-size:var( --font-size-heading-primary-aside, var(--font-size-heading-primary, var(--font-size)) )}.block-heading .heading-secondary{color:var(--color-heading-secondary, var(--color));font-weight:var(--font-weight-heading-secondary, var(--font-weight));font-size:var(--font-size-heading-secondary, var(--font-size));letter-spacing:var(--letter-spacing-heading-secondary, 0);text-transform:var(--text-transform-heading-secondary)}.col-aside .block-heading .heading-secondary{font-size:var( --font-size-heading-secondary-aside, var(--font-size-heading-secondary, var(--font-size)) )}.block-heading .heading-minor{font-weight:var(--font-weight-minor-copy, var(--font-weight));font-size:var(--font-size-minor-copy, var(--font-size));letter-spacing:var(--letter-spacing-minor-copy, 0);text-transform:var(--text-transform-minor-copy)}.col-aside .block-heading .heading-minor{font-size:var( --font-size-minor-copy-aside, var(--font-size-minor-copy, var(--font-size)) )}.block-heading-split{display:flex;flex-flow:row nowrap;gap:12px;align-items:center;justify-content:space-between}.block-heading-stacked>*+*{margin-top:var(--spacing-block-heading, 2pt)}.block-heading-stacked>*,.block-heading-split-left:not(.stacked),.block-heading-split-right:not(.stacked){display:flex;flex-flow:row wrap;align-items:center}.block-heading-split-right:not(.stacked){flex-basis:var(--width-block-heading-right, auto);justify-content:flex-end;text-align:right}.block-heading-split-left.stacked>*+*,.block-heading-split-right.stacked>*+*{margin-top:var(--spacing-block-heading-stacked, 2pt)}.block-heading .block-heading-split-left .heading-minor,.block-heading .block-heading-split-right .heading-minor{font-weight:var(--font-weight-minor-copy, var(--font-weight));font-size:var(--font-size-minor-copy, var(--font-size));text-transform:var(--text-transform-minor-copy)}.block-content{width:var(--width-block-content, auto)}.block-content>*{margin-bottom:var(--spacing-block-content, 2pt)}.block-content>*:last-of-type{margin-bottom:0}#work-experience .block-heading .heading-primary{font-weight:var( --font-weight-heading-primary-work, var(--font-weight-heading-primary, var(--font-weight)) )}#work-experience .block-heading .heading-secondary{font-weight:var( --font-weight-heading-secondary-work, var(--font-weight-heading-secondary, var(--font-weight)) )}#work-experience .block-heading .heading-minor{font-weight:var( --font-weight-minor-copy-work, var(--font-weight-minor-copy, var(--font-weight)) )}#work-experience .block-heading.muted .heading-secondary{font-weight:var( --font-weight-muted, var( --font-weight-heading-secondary-work, var(--font-weight-heading-secondary, var(--font-weight)) ) )}#work-experience .block-heading.muted .heading-minor{font-weight:var( --font-weight-muted, var( --font-weight-minor-copy-work, var(--font-weight-minor-copy, var(--font-weight)) ) )}.columns{display:flex;flex-flow:row nowrap;column-gap:var(--spacing-columns, 5%)}.columns .col-main:not(:only-child){flex:0 0 var(--width-column-main, 70%)}.section.split{position:relative}.section.split .section-title{position:absolute;top:0;left:0;width:var(--width-section-title, 1.4in);word-break:break-word}.section.split .section-content{width:calc(100% - var(--width-section-title, 1.4in));margin-left:var(--offset-section-content, 0)}.section.split>*+*{margin-top:0}ul{margin-top:var(--spacing-list-top, 0);margin-bottom:0;padding:0;list-style:none}ul.skills-columns{column-count:var(--skills-layout-columns);column-gap:0}li{display:flex;flex-flow:row nowrap;align-items:center;line-height:var(--line-height-list-item, var(--line-height))}li+li{margin-top:var(--spacing-list, 4pt)}ul li:before{align-self:baseline;padding-right:7px;padding-left:8px;font-weight:var(--bullet-weight, 200);font-size:var(--bullet-size, 11pt);content:var(--bullet-char, "•")}.col-aside ul.skills-columns{column-count:1}.skills-grid{display:flex;flex-flow:row wrap;row-gap:var(--spacing-content-blocks)}.skills-grid li{padding-right:var(--spacing-content-blocks)}.skill-category-label{padding-bottom:var(--spacing-list, 4pt);font-weight:600}.skill-category-label.inline{display:inline-block;padding-bottom:0}.skills-columns li{padding-right:var(--spacing-content-blocks)}.col-aside li{padding-right:0}.skills-grid.block>*+*{margin-top:0}.skills-grid>div{width:calc(100% / var(--skills-layout-columns))}.col-aside .skills-grid>div{width:100%}.company-description{padding-top:var(--spacing-company-description-top, 0);padding-bottom:var(--spacing-company-description-bottom, 0)}.skill-competency{padding-bottom:var(--spacing-skill-competency, 0);word-break:break-word}.cover-letter-content>*+*{margin-top:var(--spacing-cover-letter-paragraph, 1em)}.wysiwyg-content{padding-top:var(--spacing-wysiwyg-content, 0)}.date-field{white-space:nowrap}.block-heading.education-inline *{line-height:var(--line-height)}`;

export const RESUME_SEMANTIC_UTILITY_CSS = String.raw`
.resume-template {
  color: var(--color, #000);
  font-size: var(--font-size-body-copy, 10pt);
  font-weight: var(--font-weight-body-copy, 300);
  font-family: var(--font-family, Tinos), serif;
  line-height: var(--line-height, 130%);
}
/* Classic strip headings (Professional Summary, Work Experience, …): strong must read bold; parent weight 300 + UA “bolder” is often only 400 */
.resume-template .classic-section-header strong,
.resume-template .classic-section-header b {
  font-weight: 600;
}
.resume-name {
  font-size: var(--font-size-full-name, 22pt);
  font-weight: var(--font-weight-full-name, 600);
  line-height: var(--line-height-full-name, 100%);
  text-transform: var(--text-transform-full-name, none);
}
.resume-section-title {
  color: var(--accent-color, #000000);
  font-size: var(--font-size-section-title, 10pt);
  font-weight: var(--font-weight-section-title, 600);
  letter-spacing: var(--letter-spacing-section-title, 0);
  text-transform: var(--text-transform-section-title, uppercase);
}
.resume-heading-primary {
  font-size: var(--font-size-heading-primary, 12pt);
  font-weight: var(--font-weight-heading-primary, 600);
  letter-spacing: var(--letter-spacing-heading-primary, 0);
  text-transform: var(--text-transform-heading-primary, none);
}
.resume-heading-secondary {
  font-size: var(--font-size-heading-secondary, 10pt);
  font-weight: var(--font-weight-heading-secondary-work, var(--font-weight-heading-secondary, 300));
  letter-spacing: var(--letter-spacing-heading-secondary, 0);
  text-transform: var(--text-transform-heading-secondary, none);
}
.resume-minor {
  font-size: var(--font-size-minor-copy, var(--font-size-body-copy, 10pt));
  font-weight: var(--font-weight-minor-copy, 300);
  text-transform: var(--text-transform-minor-copy, none);
}
.resume-copy {
  font-size: var(--font-size-body-copy, 10pt);
  line-height: var(--line-height, 130%);
}
.resume-list-copy {
  font-size: var(--font-size-body-copy, 10pt);
  line-height: var(--line-height-list-item, var(--line-height, 130%));
}
/* TipTap / {@html} root. List markers use RESUME_STRUCTURAL_PRINT_CSS (ul/li + ::before), not Tailwind *:list-disc or a second list-style layer. */
.resume-rich-html {
  display: block;
}
/* Rich-text emphasis must be deterministic in preview and Chromium PDF output.
   Relying on the browser's relative bolder value makes bold only 400 when the
   resume body is 300, which is visually indistinguishable in several fonts. */
.resume-rich-html strong,
.resume-rich-html b {
  font-weight: 700;
}
.resume-rich-html em,
.resume-rich-html i {
  font-style: italic;
}
.resume-section {
  padding-bottom: var(--spacing-sections, 12pt);
}
.block-heading-split-right:not(.stacked) {
  margin-left: var(--date-alignment-margin-left, auto);
  justify-content: var(--date-alignment-justify, flex-end);
  text-align: var(--date-alignment-text, right);
}
.location-field {
  text-align: var(--location-alignment, left);
}
.skills-content.columns ul,
.skills-content.columns ol {
  column-count: var(--skills-layout-columns, 3);
  column-gap: 0;
}
.skills-content.columns ul li,
.skills-content.columns ol li {
  break-inside: avoid;
}
`;

export const CANVAS_PAGINATION_CSS = String.raw`html {
      overflow: hidden;
    }

    .original {
      padding: 0 var(--padding-x);
      position: fixed;
      left: -9999px;
      top: -9999px;
      width: var(--page-width, 8.5in);

      /* mobile devices often have text scaling, which breaks pagination. disable it. */
      -webkit-text-size-adjust: 100%;
      -moz-text-size-adjust: 100%;
      -ms-text-size-adjust: 100%;
      text-size-adjust: 100%;
    }

    .page {
      padding: var(--padding-y) var(--padding-x);
      position: relative;
      background-color: white;
      border-radius: 3px;
      height: var(--page-height, 11in) !important;
      width: var(--page-width, 8.5in) !important;
    }

    /* display the border without impacting element size */
    .page::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      border: 0.5px solid #ccc;
      border-radius: 3px;
      box-shadow: 0px 10px 15px -3px rgba(0, 0, 0, 0.10), 0px 4px 6px -2px rgba(0, 0, 0, 0.05);
      pointer-events: none;
    }

    .page:not(:last-child) {
      margin-bottom: 25.803168329061542px;
    }

    body {
      display: flex;
      justify-content: center;
      position: relative;
      height: 2186px;
      padding: 16px 0;
      background-color: #fafafa;
    }

    .scaled {
      width: var(--page-width, 8.5in);
      transform-origin: top;
    }`;

export const getRecoveredTemplateVars = (template?: string) => {
    switch (template) {
        case "LAYOUT2":
            return TEMPLATE_VARS_LAYOUT2;
        // Layout 3 strongly matches the centered header profile from head1.
        case "LAYOUT3":
            return TEMPLATE_VARS_LAYOUT1;
        // Layout 4 appears closest to the left-aligned Poppins profile.
        case "LAYOUT4":
            return TEMPLATE_VARS_LAYOUT3;
        // Layout 1 and Layout 4 are not perfectly distinguishable from the
        // recovered heads; defaulting Layout 1 to the same baseline for now.
        case "LAYOUT1":
            return TEMPLATE_VARS_LAYOUT3;
        default:
            return TEMPLATE_VARS_LAYOUT3;
    }
};

/**
 * Minimal CSS utility classes needed by the Classic (Resume Builder) template.
 * Equivalent to the Tailwind classes used in that template, injected into
 * both the shadow DOM preview and the PDF HTML so no Tailwind dependency is needed.
 */
export const CLASSIC_RESUME_CSS = String.raw`/* Layout */
/* Classic building blocks (keep markup structural; no utility-class system) */
.classic-builder-content {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  margin-left: auto;
  margin-right: auto;
  box-sizing: border-box;
  background: #ffffff;
}
.classic-card {
  padding: 0;
  border-radius: 6px;
}

.classic-row-between {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
}
.classic-row-between > :first-child {
  flex: 1 1 auto;
  min-width: 0;
}
.classic-row-between > :last-child {
  flex: 0 0 auto;
  width: auto;
  justify-content: flex-end;
  text-align: right;
}
.classic-row-between-start {
  display: flex;
  width: 100%;
  align-items: flex-start;
  justify-content: space-between;
}
.classic-row-between-start > :first-child {
  flex: 1 1 auto;
  min-width: 0;
}
.classic-row-between-start > :last-child {
  flex: 0 0 auto;
  width: auto;
  justify-content: flex-end;
  text-align: right;
}
.classic-row {
  display: flex;
  align-items: center;
}
.classic-row-start {
  display: flex;
  align-items: flex-start;
}
.classic-inline {
  display: inline;
}
.classic-w-full {
  width: 100%;
}

.classic-field {
  min-width: 20px;
  text-align: left;
  outline: none;
  border-bottom: 1px solid transparent;
}
.classic-field-no-border {
  border-bottom-color: transparent;
}
.classic-focus-outline-none {
  outline: none;
}

.classic-section-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: flex-start;
  padding-bottom: 1.7094px;
  border-bottom: 1.7094px solid rgb(0, 0, 0);
}
.classic-section-header-inner {
  width: fit-content;
}

.classic-entry {
  page-break-inside: avoid;
  break-inside: avoid;
  position: relative;
  width: 100%;
  border-radius: 6px;
  padding: 4px;
}

.classic-mt-8 {
  margin-top: 8px;
}
.classic-mt-4 {
  margin-top: 4px;
}

/* Classic typography helpers (avoid repeating inline styles) */
.classic-font {
  font-size: 14px;
  font-family: var(--font-family, 'Times New Roman', Times, serif);
  line-height: var(--line-height, 130%);
}
.classic-font-regular {
  font-weight: 400;
}
.classic-font-semibold {
  font-weight: 600;
}
.classic-font-italic {
  font-style: italic;
}
.classic-section-title-text {
  text-transform: uppercase;
}
.classic-date-text {
  font-weight: 400;
  font-style: italic;
  border-bottom: 1px solid transparent;
}
/* Classic major sections — matches Teal originator section-* margin-bottom */
.classic-page-section {
  margin-bottom: 16px;
}
.classic-summary-content {
  padding: 4px 4px 0;
}
/* Second+ job/degree/project/etc. row — matches originator margin-top between entries */
.classic-repeat-entry-gap {
  margin-top: 16px;
}
/* Outer PDF column — matches originator #pdf-resume-builder-content mb-10 */
.classic-builder-bottom-spacer {
  margin-bottom: 2.5rem;
}
/* Name/contact block → next block (originator ~24–30px); no p-2 so spacing isn’t doubled */
.resume-template .builder-content > .classic-header-block:first-child {
  margin-bottom: 1.5rem;
}
/*
 * Teal header: two stacked flex rows (full width, justify-center); contact row has 1px vertical padding.
 * Inner shell: flex + align-items center + position relative; name: min-width, text-left, transparent border-bottom.
 */
.resume-template .builder-content > .classic-header-block:first-child .classic-header-name-row,
.resume-template .builder-content > .classic-header-block:first-child .classic-header-contact-row {
  display: flex;
  width: 100%;
  justify-content: center;
  box-sizing: border-box;
}
.resume-template .builder-content > .classic-header-block:first-child .classic-header-name-inner,
.resume-template .builder-content > .classic-header-block:first-child .classic-header-contact-inner {
  display: flex;
  align-items: center;
  position: relative;
  max-width: 100%;
}
.resume-template .builder-content > .classic-header-block:first-child .classic-header-contact-row {
  padding-top: 1px;
  padding-bottom: 1px;
}
.resume-template .builder-content > .classic-header-block:first-child .classic-header-name-field {
  min-width: 20px;
  text-align: left;
  outline: none;
  border-bottom: 1px solid transparent;
  margin: 0;
}
.resume-template .builder-content > .classic-header-block:first-child .header a.classic-contact-link,
.resume-template .builder-content > .classic-header-block:first-child .header a.classic-contact-link:visited {
  color: #000;
  text-decoration: underline;
  text-decoration-color: #2a8176;
}
.resume-template .builder-content > .classic-header-block:first-child .header a.classic-contact-link::after,
.resume-template .builder-content > .classic-header-block:first-child .header a.classic-contact-link:visited::after {
  display: inline-block;
  text-decoration: none;
}
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='left'] .classic-header-name-row,
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='left'] .classic-header-contact-row {
  justify-content: flex-start;
}
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='right'] .classic-header-name-row,
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='right'] .classic-header-contact-row {
  justify-content: flex-end;
}
/*
 * Teal name row: shrink-wrapped block centered; text-left so wrapped lines match add-name (not per-line center).
 */
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='center'] .header-content {
  align-items: center;
}
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='center'] .full-name {
  width: fit-content;
  max-width: 100%;
  text-align: left;
}
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='center'] .contact-details {
  width: 100%;
}
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='right'] .header-content {
  align-items: flex-end;
}
.resume-template .builder-content > .classic-header-block:first-child .header[data-header-align='right'] .full-name {
  width: fit-content;
  max-width: 100%;
  text-align: right;
}
/* Only direct children of .builder-content: header ↔ summary ↔ main column.
   Does not affect Work/Education/… siblings (they live inside the third child). */
.resume-template .builder-content > * + * {
  margin-top: 1rem;
}
/* Header already provides its own bottom margin; avoid double-spacing to the next block. */
.resume-template .builder-content > .classic-header-block:first-child + * {
  margin-top: 0;
}
/* List bullets and spacing: use RESUME_STRUCTURAL_PRINT_CSS ul/li + ::before (same as Layout1).
   Do not add a second list style here — it caused double markers / extra indent in Classic. */
/* Skills list — remove double indent */
.skills-content ul,
.skills-content ol {
  padding-left: 0;
}

/* Classic rich HTML (WYSIWYG output) */
.classic-richHtml p + p {
  margin-top: 4px;
}
.classic-richHtml ul,
.classic-richHtml ol {
  margin-top: 4px;
}
.classic-richHtml li {
  align-items: flex-start;
}

/* Ordered lists: li is flex (structural CSS); native decimal markers are suppressed — mirror ul’s ::before pattern. */
.resume-template ol {
  margin-top: var(--spacing-list-top, 0);
  margin-bottom: 0;
  padding: 0;
  list-style: none;
  counter-reset: resume-ol;
}
.resume-template ol > li {
  counter-increment: resume-ol;
}
.resume-template ol > li::before {
  align-self: baseline;
  padding-right: 7px;
  padding-left: 8px;
  font-weight: var(--bullet-weight, 200);
  font-size: var(--bullet-size, 11pt);
  content: counter(resume-ol) '. ';
  flex-shrink: 0;
}

/* References spacing (Classic only) */
.classic-reference-name {
  margin-bottom: 2px;
}
.classic-reference-role {
  margin-bottom: 2px;
}
.classic-reference-email {
  line-height: 18px;
  margin-bottom: 4px;
}
.classic-reference-phone {
  line-height: 18px;
  margin-bottom: 2px;
}
`;

/**
 * PDF page-splitting rules, shared by every template.
 *
 * Chromium splits the PDF into pages by these hints, so they decide where a CV breaks. The old
 * rule kept a whole job together, so a job that did not fit the space left on a page jumped to the
 * next page and left a large gap. Now a job may split between bullets, while a heading stays with
 * its first two bullets and a job never ends with a single bullet on the next page.
 *
 * The preview does not paginate, so this only affects downloaded and emailed PDFs.
 */
export const RESUME_PAGINATION_CSS = String.raw`
/* PDF pagination. Chromium splits pages by these hints, so they decide where a CV breaks. */
/* A job with bullets may split between bullets, instead of jumping whole to the next page and leaving a gap. */
.classic-entry:has(li),
.page-break-avoid-inside:has(li) {
	break-inside: auto !important;
	page-break-inside: auto !important;
}
/* Never strand a heading from what follows it. */
.classic-section-header,
.section-title,
.block-heading,
.classic-entry > .classic-row-between {
	break-after: avoid;
	page-break-after: avoid;
}
/* Keep a job's heading with its first two bullets, keep the last two bullets together, and never split a bullet. */
.resume-rich-html li {
	break-inside: avoid;
	page-break-inside: avoid;
}
.resume-rich-html li:first-child,
.resume-rich-html li:nth-last-child(2) {
	break-after: avoid;
	page-break-after: avoid;
}
.resume-rich-html p {
	orphans: 2;
	widows: 2;
}
`;
