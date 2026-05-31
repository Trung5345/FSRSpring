---
name: Vibrant Tactile System
colors:
  surface: '#fbf9f9'
  surface-dim: '#dbdad9'
  surface-bright: '#fbf9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f3'
  surface-container: '#efeded'
  surface-container-high: '#e9e8e7'
  surface-container-highest: '#e3e2e2'
  on-surface: '#1b1c1c'
  on-surface-variant: '#3e4850'
  inverse-surface: '#303031'
  inverse-on-surface: '#f2f0f0'
  outline: '#6e7881'
  outline-variant: '#bdc8d2'
  surface-tint: '#006590'
  primary: '#006590'
  on-primary: '#ffffff'
  primary-container: '#1cb0f6'
  on-primary-container: '#00405d'
  inverse-primary: '#88ceff'
  secondary: '#755b00'
  on-secondary: '#ffffff'
  secondary-container: '#fec700'
  on-secondary-container: '#6e5400'
  tertiary: '#843ab4'
  on-tertiary: '#ffffff'
  tertiary-container: '#d087ff'
  on-tertiary-container: '#5d068e'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c8e6ff'
  primary-fixed-dim: '#88ceff'
  on-primary-fixed: '#001e2e'
  on-primary-fixed-variant: '#004c6e'
  secondary-fixed: '#ffdf92'
  secondary-fixed-dim: '#f4bf00'
  on-secondary-fixed: '#241a00'
  on-secondary-fixed-variant: '#594400'
  tertiary-fixed: '#f4d9ff'
  tertiary-fixed-dim: '#e4b5ff'
  on-tertiary-fixed: '#2f004b'
  on-tertiary-fixed-variant: '#6a1c9a'
  background: '#fbf9f9'
  on-background: '#1b1c1c'
  surface-variant: '#e3e2e2'
typography:
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '800'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '800'
    lineHeight: '1.2'
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '500'
    lineHeight: '1.5'
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: '1.5'
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  gutter: 16px
  margin-mobile: 20px
  margin-desktop: 48px
  stack-sm: 12px
  stack-md: 24px
  stack-lg: 40px
---

## Brand & Style

This design system is built to evoke energy, optimism, and a sense of "physicality" in a digital space. The brand personality is friendly and encouraging, aimed at users who value both modern aesthetics and intuitive, tactile feedback. 

The style is a hybrid of **Minimalism** and **Tactile/Skeuomorphism**. We utilize heavy whitespace and a clean layout to maintain modern clarity, but we inject personality through "squishy" interactive elements. Components should feel like physical objects that can be pressed, featuring subtle 3D depth rather than flat abstraction. This approach reduces cognitive load by making interactive zones immediately recognizable through their perceived volume and weight.

## Colors

The palette is centered around a **Vibrant Blue (#1CB0F6)**, which serves as the primary driver for action and brand identity. This blue is supported by a sunny yellow secondary and a soft purple tertiary to maintain a playful, multi-color ecosystem.

Functional colors are mapped to the blue hue:
- **Surface Container Low:** A very pale, cool-tinted azure used for background layering to keep the UI feeling airy and "wet."
- **Primary Fixed:** A soft pastel blue for large decorative surfaces that require brand presence without the high-contrast intensity of the primary action color.
- **Primary Shadow:** A slightly darker, more saturated version of the primary blue, used specifically for the bottom "edge" of tactile components to create a 3D effect.

## Typography

We use **Plus Jakarta Sans** across all levels to maintain a soft, welcoming, and contemporary feel. The type scale relies on heavy weights (Bold and ExtraBold) for headlines to anchor the "bold" brand personality.

Headlines use tighter letter spacing and lower line heights to feel like cohesive "blocks" of impact. Body text maintains a medium weight (500) to ensure legibility against vibrant backgrounds, avoiding the spindly look of lighter weights. Labels are rendered in uppercase with slight tracking to provide a clear functional distinction from narrative body text.

## Layout & Spacing

The system uses a **12-column fluid grid** for desktop and a **4-column grid** for mobile. The spacing rhythm is strictly based on an **8px base unit**, ensuring mathematical harmony across all components.

Layouts should prioritize vertical "stacks." Instead of complex nested grids, use generous vertical spacing (`stack-lg`) to separate content sections, allowing the tactile components room to "breathe" and cast their shadows without overlapping. Margins on mobile are slightly oversized (20px) to prevent thumbs from hitting the screen edges and to reinforce the friendly, inset look of the containers.

## Elevation & Depth

Depth is not communicated through blurry ambient shadows, but through **Tonal Layers** and **Extruded Geometry**. 

1.  **Level 0 (Floor):** The base background using `surface-container-low`.
2.  **Level 1 (Cards):** Flat surfaces with a 2px solid border in a slightly darker neutral shade to define boundaries.
3.  **Level 2 (Tactile Elements):** Interactive elements (buttons, chips) feature a "thick" bottom border (usually 4px) that mimics a physical plastic button. 

When an element is pressed, the vertical offset and the bottom shadow should decrease or disappear, simulating the physical displacement of the button into the surface.

## Shapes

The shape language is consistently **Rounded**. 

- Standard components (Inputs, Cards) use a **0.5rem (8px)** radius.
- Large containers use **1rem (16px)**.
- Interactive triggers like buttons and chips should feel "bubbly" and are encouraged to use **Pill-shaped (fully rounded)** corners to maximize the friendly, toy-like aesthetic. 

Avoid sharp 90-degree angles entirely, as they conflict with the approachable brand narrative.

## Components

### Buttons
Buttons are the hero of this system. They must feature a "3D Lip"—a solid bottom border 4px thick in a darker shade of the button's background color. On `:active` states, the button should translate 2px downward and the lip should shrink, providing instant tactile feedback.

### Cards
Cards should be simple and clean, using a white background with a subtle `1px` border in a soft cool grey. They do not use shadows; instead, they rely on the `surface-container-low` background to provide contrast.

### Input Fields
Fields use a thick 2px border that darkens when focused. The inner background should be slightly inset using a very subtle inner-shadow effect to look like a "well" that information is poured into.

### Chips & Tags
Chips are fully pill-shaped. For selection states, they should adopt the Primary Blue background but maintain a smaller 2px version of the tactile "lip."

### Lists
List items are separated by generous gaps rather than thin lines. Each item should feel like a "tile" floating on the `surface-container-low` floor.