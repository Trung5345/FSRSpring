    tailwind.config = {
      darkMode:"class",
      theme:{ extend:{
        colors:{
          "outline":"#6e7881","primary-fixed":"#c8e6ff","primary-fixed-dim":"#88ceff",
          "surface-container-lowest":"#ffffff","surface-container":"#efeded",
          "surface-container-high":"#e9e8e7","surface-container-highest":"#e3e2e2",
          "surface-container-low":"#f5f3f3","surface":"#fbf9f9","surface-variant":"#e3e2e2",
          "on-error":"#ffffff","secondary-container":"#fec700","background":"#fbf9f9",
          "error-container":"#ffdad6","outline-variant":"#bdc8d2","secondary-fixed-dim":"#f4bf00",
          "secondary-fixed":"#ffdf92","on-primary-fixed":"#001e2e","primary":"#006590",
          "on-surface-variant":"#3e4850","on-background":"#1b1c1c","on-surface":"#1b1c1c",
          "on-secondary-container":"#6e5400","on-primary":"#ffffff","on-primary-container":"#00405d",
          "primary-container":"#1cb0f6","on-error-container":"#93000a","inverse-primary":"#88ceff",
          "error":"#ba1a1a","tertiary":"#bb1522","on-tertiary":"#ffffff",
        },
        borderRadius:{ DEFAULT:"0.25rem", lg:"0.5rem", xl:"0.75rem", full:"9999px" },
        spacing:{ xs:"8px", sm:"16px", md:"24px", lg:"32px", xl:"48px", gutter:"16px" },
        fontFamily:{
          "button-text":["Lexend","sans-serif"],"headline-md":["Lexend","sans-serif"],
          "headline-lg":["Lexend","sans-serif"],"body-md":["Nunito Sans","sans-serif"],
          "label-lg":["Lexend","sans-serif"],"body-lg":["Nunito Sans","sans-serif"]
        },
        fontSize:{
          "button-text":["17px",{lineHeight:"1.0",letterSpacing:"0.02em",fontWeight:"700"}],
          "headline-md":["24px",{lineHeight:"1.2",letterSpacing:"-0.01em",fontWeight:"700"}],
          "headline-lg":["32px",{lineHeight:"1.2",letterSpacing:"-0.02em",fontWeight:"700"}],
          "body-md":["17px",{lineHeight:"1.5",fontWeight:"500"}],
          "label-lg":["15px",{lineHeight:"1.0",letterSpacing:"0.05em",fontWeight:"700"}],
          "body-lg":["19px",{lineHeight:"1.5",fontWeight:"500"}]
        }
      }}
    }
  </script>
