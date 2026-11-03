# SFCDelight Phase 2 — Night-Market Equipment

- Core 3×1×2 cart: six active multipart blocks; 12 edible display slots, pepper/chili storage and eight basket racks left untouched.
- Fryer: 7 stored indicator configurations (0–5 active, 6 legacy), five fuel stages and oil surface positions preserved; cooking basket remain a separate block above the oil.
- Open-top 8-second de-oiler: transparent physical food view, 3D wire rack and front instrument panel; no new logic or scheduled tasks.
- Two-block preparation counter: wooden board and stainless sink with the original water/food render coordinates preserved.
- Plastic food display tray: detailed low 3D mesh, still used in the cart via `ItemDisplayContext.FIXED`.

Material palette: 13 original 16×16 RGBA pixel textures under `assets/sfcdelight/textures/block/nightmarket_*.png`.

**Version:** `0.2.48-alpha`. Existing mod item IDs, block states, recipes, inventory sizes, content data and server code are unchanged. Automated tests check structural model coordinates and item/texture references. Actual appearance, click positions and lighting still need a game client test.
