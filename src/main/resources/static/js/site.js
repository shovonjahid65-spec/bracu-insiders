/* Progressive enhancement only. Publishing, navigation and messaging are server-rendered. */
document.documentElement.classList.add("js");
const dropdowns = Array.from(document.querySelectorAll("[data-drop]"));
function closeMenus(except) {
  for (const menu of dropdowns) if (menu !== except) menu.open = false;
}
for (const menu of dropdowns) {
  menu.addEventListener("toggle", () => {
    if (!menu.open) return;
    closeMenus(menu);
    menu.querySelector("input[type=search]")?.focus();
  });
}
document.addEventListener("click", (event) => {
  if (!event.target.closest("[data-drop]")) closeMenus();
  const button = event.target.closest("[data-confirm]");
  if (button && !window.confirm(button.dataset.confirm)) event.preventDefault();
});
document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") return;
  const open = dropdowns.find((menu) => menu.open);
  if (open) {
    open.open = false;
    open.querySelector("summary").focus();
  }
});
for (const button of document.querySelectorAll("[data-copy-url], [data-copy-receipt]")) {
  button.addEventListener("click", async () => {
    const receipt = document.querySelector("#receipt-code");
    const value = button.hasAttribute("data-copy-receipt") ? receipt.textContent : window.location.href;
    const status = document.querySelector("[data-copy-status]");
    try {
      await navigator.clipboard.writeText(value);
      status.textContent = "Copied.";
    } catch {
      if (receipt) {
        const range = document.createRange();
        range.selectNodeContents(receipt);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
      }
      status.textContent = receipt
        ? "Select and copy the code above."
        : "Copy the address from your browser.";
    }
  });
}
