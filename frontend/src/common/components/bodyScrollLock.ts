interface OriginalBodyStyles {
  overflow: string;
  paddingRight: string;
  paddingRightPriority: string;
}

let lockCount = 0;
let originalBodyStyles: OriginalBodyStyles | null = null;

/** Locks background scrolling once, even when multiple shared dialogs are mounted. */
export function acquireBodyScrollLock(): () => void {
  if (typeof document === "undefined") return () => {};

  if (lockCount === 0) {
    const body = document.body;
    originalBodyStyles = {
      overflow: body.style.overflow,
      paddingRight: body.style.getPropertyValue("padding-right"),
      paddingRightPriority: body.style.getPropertyPriority("padding-right"),
    };

    const scrollbarWidth = Math.max(0, window.innerWidth - document.documentElement.clientWidth);
    const supportsStableGutter = window.CSS?.supports("scrollbar-gutter: stable") ?? false;

    // Modern browsers keep the viewport gutter through the global CSS rule. Older
    // browsers need a body padding compensation to prevent the content reflow.
    if (!supportsStableGutter && scrollbarWidth > 0) {
      const currentPadding = Number.parseFloat(window.getComputedStyle(body).paddingRight) || 0;
      body.style.setProperty("padding-right", `${currentPadding + scrollbarWidth}px`);
    }
    body.style.overflow = "hidden";
  }

  lockCount += 1;
  let released = false;

  return () => {
    if (released) return;
    released = true;
    lockCount = Math.max(0, lockCount - 1);
    if (lockCount > 0 || !originalBodyStyles) return;

    const body = document.body;
    body.style.overflow = originalBodyStyles.overflow;
    if (originalBodyStyles.paddingRight) {
      body.style.setProperty("padding-right", originalBodyStyles.paddingRight, originalBodyStyles.paddingRightPriority);
    } else {
      body.style.removeProperty("padding-right");
    }
    originalBodyStyles = null;
  };
}
