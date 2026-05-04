(function () {
    "use strict";

    const data = window.ParkNetMapData || {};
    const districtCenters = Array.isArray(data.districtCenters) ? data.districtCenters : [];
    const sofiaCenter = Array.isArray(data.sofiaCenter) ? data.sofiaCenter : [42.6977, 23.3219];
    const initialListings = Array.isArray(data.listings) ? data.listings : [];
    const isAuthenticated = Boolean(data.authenticated);
    const loginUrl = data.loginUrl || "/login";
    const createListingUrl = data.createListingUrl || "/api/listings/map";
    const csrfParameterName = data.csrfParameterName || "_csrf";
    const csrfToken = data.csrfToken || "";
    const SOFIA_BOUNDS = {
        minLatitude: 42.55,
        maxLatitude: 42.85,
        minLongitude: 23.05,
        maxLongitude: 23.65
    };
    const SOFIA_MAP_BOUNDS = [
        [SOFIA_BOUNDS.minLatitude, SOFIA_BOUNDS.minLongitude],
        [SOFIA_BOUNDS.maxLatitude, SOFIA_BOUNDS.maxLongitude]
    ];
    const NEIGHBORHOODS = {
        CENTER: {name: "Център", district: "CENTER", center: [42.690, 23.319], zoom: 17, bounds: [[42.684, 23.310], [42.696, 23.328]]},
        LOZENETS: {name: "Лозенец", district: "LOZENETS", center: [42.675, 23.320], zoom: 17, bounds: [[42.667, 23.312], [42.683, 23.328]]},
        MLADOST: {name: "Младост 1", district: "MLADOST", center: [42.650, 23.380], zoom: 17, bounds: [[42.642, 23.370], [42.657, 23.390]]},
        STUDENTSKI: {name: "Студентски град", district: "STUDENTSKI", center: [42.650, 23.345], zoom: 17, bounds: [[42.644, 23.336], [42.656, 23.354]]},
        LYULIN: {name: "Люлин", district: "LYULIN", center: [42.716, 23.250], zoom: 17, bounds: [[42.710, 23.241], [42.724, 23.259]]},
        OBORISHTE: {name: "Оборище", district: "OBORISHTE", center: [42.697, 23.342], zoom: 17, bounds: [[42.691, 23.333], [42.703, 23.351]]},
        KRASNO_SELO: {name: "Красно село", district: "KRASNO_SELO", center: [42.681, 23.285], zoom: 17, bounds: [[42.675, 23.276], [42.687, 23.294]]},
        PODUYANE: {name: "Подуяне", district: "PODUYANE", center: [42.708, 23.350], zoom: 17, bounds: [[42.702, 23.341], [42.714, 23.359]]},
        GEO_MILEV: {name: "Гео Милев", district: "GEO_MILEV", center: [42.681, 23.365], zoom: 17, bounds: [[42.675, 23.356], [42.687, 23.374]]},
        MANASTIRSKI_LIVADI: {name: "Манастирски ливади", district: "MANASTIRSKI_LIVADI", center: [42.660, 23.295], zoom: 17, bounds: [[42.653, 23.286], [42.667, 23.304]]},
        IVAN_VAZOV: {name: "Иван Вазов", district: "IVAN_VAZOV", center: [42.678, 23.308], zoom: 17, bounds: [[42.672, 23.300], [42.684, 23.316]]},
        BOROVO: {name: "Борово", district: "BOROVO", center: [42.670, 23.285], zoom: 17, bounds: [[42.664, 23.276], [42.676, 23.294]]},
        DIANABAD: {name: "Дианабад", district: "DIANABAD", center: [42.671, 23.352], zoom: 17, bounds: [[42.665, 23.343], [42.677, 23.361]]},
        DRUZHBA: {name: "Дружба 1", district: "DRUZHBA", center: [42.666, 23.397], zoom: 17, bounds: [[42.660, 23.388], [42.672, 23.406]]},
        NADEZHDA: {name: "Надежда", district: "NADEZHDA", center: [42.727, 23.303], zoom: 17, bounds: [[42.721, 23.294], [42.733, 23.312]]},
        BANISHORA: {name: "Банишора", district: "BANISHORA", center: [42.711, 23.315], zoom: 17, bounds: [[42.705, 23.306], [42.717, 23.324]]},
        OVCHA_KUPEL: {name: "Овча купел", district: "OVCHA_KUPEL", center: [42.676, 23.255], zoom: 17, bounds: [[42.670, 23.246], [42.682, 23.264]]},
        GOTSE_DELCHEV: {name: "Гоце Делчев", district: "GOTSE_DELCHEV", center: [42.665, 23.292], zoom: 17, bounds: [[42.659, 23.283], [42.671, 23.301]]},
        IZTOK: {name: "Изток", district: "IZTOK", center: [42.667, 23.351], zoom: 17, bounds: [[42.661, 23.342], [42.673, 23.360]]},
        SVETA_TROITSA: {name: "Света Троица", district: "SVETA_TROITSA", center: [42.704, 23.289], zoom: 17, bounds: [[42.698, 23.280], [42.710, 23.298]]}
    };
    const COLORS = {
        available: "#1f7a5b",
        requested: "#b7791f",
        booked: "#b3261e",
        unavailable: "#b3261e",
        owned: "#5b5bb7",
        inactive: "#69717a",
        cheap: "#1f7a5b",
        medium: "#c58a1d",
        expensive: "#c23b68"
    };

    const state = {
        map: null,
        dotLayer: null,
        polygonLayer: null,
        labelLayer: null,
        drawingLayer: null,
        listings: initialListings,
        layersById: new Map(),
        selectedId: null,
        hoveredId: null,
        activeNeighborhoodCode: "",
        colorMode: "availability",
        rateMode: "hourly",
        drawing: {
            active: false,
            mode: null,
            targetListingId: null,
            points: [],
            geometryGeoJson: null
        },
        fetchController: null
    };

    document.addEventListener("DOMContentLoaded", initializeMapPage);

    function initializeMapPage() {
        const mapElement = document.getElementById("sofia-map");
        if (!mapElement) {
            return;
        }
        if (!window.L) {
            showMapLibraryFallback(mapElement);
            return;
        }

        state.map = L.map(mapElement, {
            zoomControl: false,
            scrollWheelZoom: true,
            maxBounds: L.latLngBounds(SOFIA_MAP_BOUNDS).pad(0.35),
            maxBoundsViscosity: 0.82,
            preferCanvas: true
        }).setView(sofiaCenter, 12);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(state.map);

        state.dotLayer = L.layerGroup().addTo(state.map);
        state.polygonLayer = L.layerGroup().addTo(state.map);
        state.labelLayer = L.layerGroup().addTo(state.map);
        state.drawingLayer = L.layerGroup().addTo(state.map);

        window.ParkNetSelectListing = (listingId) => selectListing(String(listingId), {pan: true});
        window.ParkNetRefreshMapListings = () => fetchMapListings({
            fit: !state.activeNeighborhoodCode,
            updateUrl: false,
            focusNeighborhood: state.activeNeighborhoodCode
        });

        bindControls();
        initializeNeighborhoodSelection();
        renderPreviewList();
        renderMapListings({fit: true});
        fetchMapListings({
            fit: !state.activeNeighborhoodCode,
            updateUrl: false,
            focusNeighborhood: state.activeNeighborhoodCode
        });
        if (state.activeNeighborhoodCode) {
            showPreviewListPanel();
            focusNeighborhood(state.activeNeighborhoodCode);
        }
        setTimeout(() => state.map.invalidateSize(), 80);
    }

    function showMapLibraryFallback(mapElement) {
        mapElement.innerHTML = `
            <div class="map-library-fallback">
                <strong>Картата не можа да се зареди.</strong>
                <span>Презаредете страницата или стартирайте приложението през Spring Boot.</span>
            </div>
        `;
        document.querySelector(".parknet-map-app")?.classList.add("map-unavailable");
    }

    function bindControls() {
        state.map.on("zoomend", () => renderMapListings());
        state.map.on("click", handleMapDrawingClick);

        const colorModeSelect = document.querySelector("[data-color-mode]");
        if (colorModeSelect) {
            colorModeSelect.addEventListener("change", () => {
                state.colorMode = colorModeSelect.value;
                renderMapListings();
            });
        }

        bindPanelDock();
        bindFilterForm();
        bindNeighborhoodSelector();
        bindCreationFlow();
        bindDrawingControls();
        bindBoundaryEditing();
        bindMapTools();
        bindEmptyActions();
        bindOnboardingHint();
        bindPreviewClose();
        initializeResponsiveDetails();

        document.addEventListener("parknet:listings-changed", () => {
            fetchMapListings({
                fit: !state.activeNeighborhoodCode,
                updateUrl: false,
                focusNeighborhood: state.activeNeighborhoodCode
            });
        });
    }

    function bindPanelDock() {
        const panels = Array.from(document.querySelectorAll("[data-map-panel]"));
        const toggles = Array.from(document.querySelectorAll("[data-panel-toggle]"));
        if (!panels.length || !toggles.length) {
            return;
        }

        panels.forEach((panel) => setUtilityPanelOpen(panel.dataset.mapPanel, false));

        toggles.forEach((button) => {
            button.addEventListener("click", () => {
                const panelName = button.dataset.panelToggle;
                const panel = utilityPanelByName(panelName);
                if (!panel) {
                    return;
                }
                const shouldOpen = !panel.classList.contains("is-open");
                closeUtilityPanels(panelName);
                setUtilityPanelOpen(panelName, shouldOpen);
                if (shouldOpen) {
                    dismissOnboardingHint();
                    if (panelName === "results") {
                        renderPreviewList();
                    }
                    if (panelName === "legend") {
                        updateLegend();
                    }
                }
            });
        });

        document.querySelectorAll("[data-panel-close]").forEach((button) => {
            button.addEventListener("click", () => {
                const panel = button.closest("[data-map-panel]");
                if (panel) {
                    setUtilityPanelOpen(panel.dataset.mapPanel, false);
                }
            });
        });

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeUtilityPanels();
            }
        });
    }

    function utilityPanelByName(panelName) {
        return Array.from(document.querySelectorAll("[data-map-panel]"))
            .find((panel) => panel.dataset.mapPanel === panelName) || null;
    }

    function setUtilityPanelOpen(panelName, open) {
        const panel = utilityPanelByName(panelName);
        const button = document.querySelector(`[data-panel-toggle="${panelName}"]`);
        if (panel) {
            panel.classList.toggle("is-open", Boolean(open));
            panel.setAttribute("aria-hidden", String(!open));
            panel.inert = !open;
        }
        if (button) {
            button.classList.toggle("is-active", Boolean(open));
            button.setAttribute("aria-expanded", String(Boolean(open)));
        }
    }

    function closeUtilityPanels(exceptPanelName) {
        document.querySelectorAll("[data-map-panel]").forEach((panel) => {
            const panelName = panel.dataset.mapPanel;
            if (exceptPanelName && panelName === exceptPanelName) {
                return;
            }
            setUtilityPanelOpen(panelName, false);
        });
    }

    function bindFilterForm() {
        const form = document.querySelector("[data-map-filter-form]");
        if (!form) {
            return;
        }

        let timer = null;
        const scheduleFetch = () => {
            window.clearTimeout(timer);
            timer = window.setTimeout(() => {
                fetchMapListings({
                    fit: !state.activeNeighborhoodCode,
                    updateUrl: true,
                    focusNeighborhood: state.activeNeighborhoodCode
                });
            }, 260);
        };

        form.addEventListener("submit", (event) => {
            event.preventDefault();
            closeUtilityPanels();
            fetchMapListings({
                fit: !state.activeNeighborhoodCode,
                updateUrl: true,
                focusNeighborhood: state.activeNeighborhoodCode
            });
        });

        form.querySelectorAll("input, select").forEach((control) => {
            if (control.matches("[data-district-select]")) {
                return;
            }
            control.addEventListener("change", scheduleFetch);
            if (control.type === "number") {
                control.addEventListener("input", scheduleFetch);
            }
        });
    }

    function initializeNeighborhoodSelection() {
        const selector = document.querySelector("[data-district-select]");
        state.activeNeighborhoodCode = selector ? selector.value || "" : "";
        syncNeighborhoodUi();
    }

    function bindNeighborhoodSelector() {
        const selector = document.querySelector("[data-district-select]");
        if (selector) {
            selector.addEventListener("change", () => {
                selectNeighborhood(selector.value || "", {updateUrl: true});
            });
        }

        document.querySelectorAll("[data-neighborhood-chip]").forEach((chip) => {
            chip.addEventListener("click", () => {
                selectNeighborhood(chip.dataset.neighborhoodChip || "", {updateUrl: true});
            });
        });
    }

    function selectNeighborhood(code, options) {
        dismissOnboardingHint();
        state.activeNeighborhoodCode = code || "";
        state.selectedId = null;
        state.hoveredId = null;
        syncDistrictSelect(state.activeNeighborhoodCode);
        syncNeighborhoodUi();
        closeUtilityPanels();
        showPreviewListPanel();

        if (state.activeNeighborhoodCode) {
            focusNeighborhood(state.activeNeighborhoodCode);
            fetchMapListings({
                fit: false,
                updateUrl: Boolean(options && options.updateUrl),
                focusNeighborhood: state.activeNeighborhoodCode
            });
        } else {
            fetchMapListings({
                fit: true,
                updateUrl: Boolean(options && options.updateUrl),
                focusNeighborhood: ""
            });
        }
    }

    function syncDistrictSelect(code) {
        const selector = document.querySelector("[data-district-select]");
        if (selector && selector.value !== code) {
            selector.value = code;
        }
    }

    function syncNeighborhoodUi() {
        const activeCode = state.activeNeighborhoodCode || "";
        document.querySelectorAll("[data-neighborhood-chip]").forEach((chip) => {
            const selected = (chip.dataset.neighborhoodChip || "") === activeCode;
            chip.classList.toggle("is-active", selected);
            chip.setAttribute("aria-selected", String(selected));
        });

        const summary = document.querySelector("[data-neighborhood-summary]");
        if (summary) {
            summary.textContent = activeCode ? NEIGHBORHOODS[activeCode]?.name || "Избран квартал" : "Всички квартали";
        }
    }

    function focusNeighborhood(code) {
        const neighborhood = NEIGHBORHOODS[code] || neighborhoodFromGlobalCenters(code);
        if (!neighborhood) {
            return;
        }
        state.map.flyTo(neighborhood.center, neighborhood.zoom || 16, {duration: 0.65});
    }

    function neighborhoodFromGlobalCenters(code) {
        const center = districtCenters.find((district) => district.code === code);
        if (!center) {
            return null;
        }
        const latitude = Number(center.latitude);
        const longitude = Number(center.longitude);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
            return null;
        }
        return {
            name: center.displayName || code,
            district: code,
            center: [latitude, longitude],
            zoom: 16
        };
    }

    function bindCreationFlow() {
        const createButton = document.querySelector("[data-map-create-button]");
        const createForm = document.querySelector("[data-create-form]");
        if (createButton) {
            createButton.addEventListener("click", () => {
                closeUtilityPanels();
                if (!isAuthenticated) {
                    showToast("Влезте в профила си, за да добавите паркомясто.", "Вход", loginUrl);
                    return;
                }
                startDrawingMode("create");
            });
        }

        if (createForm) {
            createForm.addEventListener("submit", submitCreateForm);
        }

        document.querySelectorAll("[data-create-close], [data-create-cancel]").forEach((button) => {
            button.addEventListener("click", cancelCreateFlow);
        });

        const redrawButton = document.querySelector("[data-create-redraw]");
        if (redrawButton) {
            redrawButton.addEventListener("click", () => startDrawingMode("create"));
        }
    }

    function bindDrawingControls() {
        const finishButton = document.querySelector("[data-drawing-finish]");
        const cancelButton = document.querySelector("[data-drawing-cancel]");
        const redrawButton = document.querySelector("[data-drawing-redraw]");
        const saveButton = document.querySelector("[data-drawing-save]");
        const rectangleButton = document.querySelector("[data-drawing-rectangle]");

        if (finishButton) {
            finishButton.addEventListener("click", finishDrawing);
        }
        if (cancelButton) {
            cancelButton.addEventListener("click", cancelDrawingFlow);
        }
        if (redrawButton) {
            redrawButton.addEventListener("click", () => startDrawingMode(state.drawing.mode || "create", selectedListing()));
        }
        if (saveButton) {
            saveButton.addEventListener("click", submitGeometryUpdate);
        }
        if (rectangleButton) {
            rectangleButton.addEventListener("click", createRectangleAtMapCenter);
        }
    }

    function bindBoundaryEditing() {
        const editBoundaryButton = document.querySelector("[data-preview-edit-boundary]");
        if (editBoundaryButton) {
            editBoundaryButton.addEventListener("click", () => {
                const listing = selectedListing();
                if (!listing || !listing.ownedByCurrentUser) {
                    return;
                }
                startDrawingMode("edit", listing);
            });
        }
    }

    function startDrawingMode(mode, listing) {
        if (!isAuthenticated) {
            showToast("Влезте в профила си, за да добавите паркомясто.", "Вход", loginUrl);
            return;
        }
        dismissOnboardingHint();
        closeUtilityPanels();

        closeCreatePanel();
        clearDrawingLayer();
        state.drawing.active = true;
        state.drawing.mode = mode;
        state.drawing.targetListingId = listing ? String(listing.id) : null;
        state.drawing.points = [];
        state.drawing.geometryGeoJson = null;
        document.querySelector(".parknet-map-app")?.classList.add("is-drawing");
        if (state.map.doubleClickZoom) {
            state.map.doubleClickZoom.disable();
        }
        showDrawingPanel();
        updateDrawingStatus("Кликнете върху картата, за да добавите първата точка.");
        updateDrawingButtons();
        if (mode === "create") {
            clearSelection();
        }
    }

    function handleMapDrawingClick(event) {
        if (!state.drawing.active || state.drawing.geometryGeoJson) {
            return;
        }
        const latitude = event.latlng.lat;
        const longitude = event.latlng.lng;
        if (!isInsideSofia(latitude, longitude)) {
            updateDrawingStatus("Точката е извън границите на София.");
            showToast("Невалидно очертание: точката е извън София.");
            return;
        }
        state.drawing.points.push({latitude: latitude, longitude: longitude});
        renderDrawingPreview();
    }

    function renderDrawingPreview() {
        clearDrawingLayer();
        const latLngs = state.drawing.points.map((point) => [point.latitude, point.longitude]);
        latLngs.forEach((latLng) => {
            L.marker(latLng, {
                interactive: false,
                icon: L.divIcon({
                    className: "",
                    html: '<span class="drawing-point-marker"></span>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                })
            }).addTo(state.drawingLayer);
        });

        if (latLngs.length >= 3) {
            L.polygon(latLngs, drawingStyle()).addTo(state.drawingLayer);
        } else if (latLngs.length >= 2) {
            L.polyline(latLngs, drawingStyle()).addTo(state.drawingLayer);
        }

        updateDrawingStatus(latLngs.length < 3
            ? `${latLngs.length} ${latLngs.length === 1 ? "точка" : "точки"} избрани. Добавете поне 3 точки.`
            : `${latLngs.length} точки избрани. Натиснете "Готово", когато очертанието е правилно.`);
        updateDrawingButtons();
    }

    function finishDrawing() {
        if (state.drawing.points.length < 3) {
            updateDrawingStatus("Добавете поне 3 точки, за да завършите очертанието.");
            showToast("Добавете поне 3 точки.");
            return;
        }
        const geometryGeoJson = geometryFromDrawingPoints();
        const validationMessage = validateGeometryClientSide(geometryGeoJson);
        if (validationMessage) {
            updateDrawingStatus(validationMessage);
            showToast(validationMessage);
            return;
        }

        state.drawing.active = false;
        state.drawing.geometryGeoJson = geometryGeoJson;
        drawTemporaryGeometry(geometryGeoJson);
        document.querySelector(".parknet-map-app")?.classList.remove("is-drawing");
        if (state.map.doubleClickZoom) {
            state.map.doubleClickZoom.enable();
        }

        if (state.drawing.mode === "edit") {
            updateDrawingStatus("Новите граници са готови. Запазете ги или начертайте отново.");
            updateDrawingButtons();
            return;
        }

        hideDrawingPanel();
        openCreatePanel(geometryGeoJson);
    }

    function createRectangleAtMapCenter() {
        if (!state.drawing.active) {
            return;
        }
        const center = state.map.getCenter();
        if (!isInsideSofia(center.lat, center.lng)) {
            updateDrawingStatus("Центърът на картата е извън границите на София.");
            showToast("Мястото трябва да е в границите на София.");
            return;
        }
        const south = clamp(center.lat - 0.00010, SOFIA_BOUNDS.minLatitude, SOFIA_BOUNDS.maxLatitude);
        const north = clamp(center.lat + 0.00010, SOFIA_BOUNDS.minLatitude, SOFIA_BOUNDS.maxLatitude);
        const west = clamp(center.lng - 0.00016, SOFIA_BOUNDS.minLongitude, SOFIA_BOUNDS.maxLongitude);
        const east = clamp(center.lng + 0.00016, SOFIA_BOUNDS.minLongitude, SOFIA_BOUNDS.maxLongitude);
        state.drawing.points = [
            {latitude: south, longitude: west},
            {latitude: south, longitude: east},
            {latitude: north, longitude: east},
            {latitude: north, longitude: west}
        ];
        renderDrawingPreview();
        finishDrawing();
    }

    function cancelDrawingFlow() {
        closeCreatePanel();
        clearDrawingLayer();
        state.drawing.active = false;
        state.drawing.mode = null;
        state.drawing.targetListingId = null;
        state.drawing.points = [];
        state.drawing.geometryGeoJson = null;
        hideDrawingPanel();
        document.querySelector(".parknet-map-app")?.classList.remove("is-drawing");
        if (state.map.doubleClickZoom) {
            state.map.doubleClickZoom.enable();
        }
        renderMapListings();
    }

    function cancelCreateFlow() {
        closeCreatePanel();
        clearDrawingLayer();
        state.drawing.mode = null;
        state.drawing.geometryGeoJson = null;
    }

    function showDrawingPanel() {
        const panel = document.querySelector("[data-drawing-panel]");
        const kicker = document.querySelector("[data-drawing-kicker]");
        const title = document.querySelector("[data-drawing-title]");
        const help = document.querySelector("[data-drawing-help]");
        if (panel) {
            panel.hidden = false;
        }
        if (kicker) {
            kicker.textContent = state.drawing.mode === "edit" ? "Редакция" : "Ново място";
        }
        if (title) {
            title.textContent = state.drawing.mode === "edit"
                ? "Очертайте новите граници на паркомястото."
                : "Очертайте границите на вашето паркомясто върху картата.";
        }
        if (help) {
            help.textContent = "Очертайте мястото върху картата. След това попълнете данните за обявата.";
        }
    }

    function hideDrawingPanel() {
        const panel = document.querySelector("[data-drawing-panel]");
        if (panel) {
            panel.hidden = true;
        }
    }

    function updateDrawingStatus(message) {
        const status = document.querySelector("[data-drawing-status]");
        if (status) {
            status.textContent = message;
        }
    }

    function updateDrawingButtons() {
        const finishButton = document.querySelector("[data-drawing-finish]");
        const saveButton = document.querySelector("[data-drawing-save]");
        const redrawButton = document.querySelector("[data-drawing-redraw]");
        const rectangleButton = document.querySelector("[data-drawing-rectangle]");
        const canFinish = state.drawing.active && state.drawing.points.length >= 3;
        const hasFinishedEdit = !state.drawing.active
                && state.drawing.mode === "edit"
                && Boolean(state.drawing.geometryGeoJson);

        if (finishButton) {
            finishButton.hidden = hasFinishedEdit;
            finishButton.disabled = !canFinish;
        }
        if (saveButton) {
            saveButton.hidden = !hasFinishedEdit;
        }
        if (redrawButton) {
            redrawButton.hidden = state.drawing.active || !state.drawing.geometryGeoJson;
        }
        if (rectangleButton) {
            rectangleButton.hidden = !state.drawing.active;
        }
    }

    function openCreatePanel(geometryGeoJson) {
        const panel = document.querySelector("[data-create-panel]");
        const form = document.querySelector("[data-create-form]");
        const geometryInput = document.querySelector("[data-create-geometry]");
        if (!panel || !form || !geometryInput) {
            return;
        }
        closePreviewPanel();
        clearCreateError();
        form.reset();
        geometryInput.value = geometryGeoJson;
        const districtSelect = document.querySelector("[data-create-district]");
        if (districtSelect && state.activeNeighborhoodCode) {
            districtSelect.value = state.activeNeighborhoodCode;
        }
        const pricingType = form.elements.pricingType;
        if (pricingType) {
            pricingType.value = "HOURLY";
        }
        panel.hidden = false;
        setTimeout(() => form.querySelector("input[name='title']")?.focus(), 40);
    }

    function closeCreatePanel() {
        const panel = document.querySelector("[data-create-panel]");
        if (panel) {
            panel.hidden = true;
        }
        clearCreateError();
    }

    function submitCreateForm(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const errorMessage = validateCreateForm(form);
        if (errorMessage) {
            showCreateError(errorMessage);
            return;
        }

        const submitButton = form.querySelector("[data-create-submit]");
        if (submitButton) {
            submitButton.disabled = true;
        }
        const formData = new FormData(form);
        if (csrfToken && !formData.has(csrfParameterName)) {
            formData.append(csrfParameterName, csrfToken);
        }

        fetch(createListingUrl, {
            method: "POST",
            credentials: "same-origin",
            headers: csrfHeaders(),
            body: formData
        })
            .then(parseJsonResponse)
            .then((listing) => {
                cancelDrawingFlow();
                closeCreatePanel();
                showToast("Паркомястото е добавено успешно.");
                fetchMapListings({
                    fit: false,
                    updateUrl: false,
                    includeListing: listing,
                    selectId: String(listing.id),
                    zoom: 17
                });
            })
            .catch((error) => {
                showCreateError(errorMessageFromResponse(error));
            })
            .finally(() => {
                if (submitButton) {
                    submitButton.disabled = false;
                }
            });
    }

    function submitGeometryUpdate() {
        const listingId = state.drawing.targetListingId;
        const geometryGeoJson = state.drawing.geometryGeoJson;
        if (!listingId || !geometryGeoJson) {
            updateDrawingStatus("Начертайте нови граници преди запис.");
            return;
        }
        const formData = new FormData();
        formData.append("geometryGeoJson", geometryGeoJson);
        if (csrfToken) {
            formData.append(csrfParameterName, csrfToken);
        }

        fetch(`/api/listings/${listingId}/geometry`, {
            method: "POST",
            credentials: "same-origin",
            headers: csrfHeaders(),
            body: formData
        })
            .then(parseJsonResponse)
            .then((listing) => {
                clearDrawingLayer();
                hideDrawingPanel();
                state.drawing.active = false;
                state.drawing.mode = null;
                state.drawing.targetListingId = null;
                state.drawing.points = [];
                state.drawing.geometryGeoJson = null;
                upsertListing(listing);
                renderMapListings();
                showToast("Границите са обновени успешно.");
                selectListing(String(listing.id), {pan: true, zoom: 17});
            })
            .catch((error) => {
                updateDrawingStatus(errorMessageFromResponse(error));
            });
    }

    function validateCreateForm(form) {
        const geometry = form.elements.geometryGeoJson?.value;
        const geometryError = validateGeometryClientSide(geometry);
        if (geometryError) {
            return geometryError;
        }
        if (!form.elements.title.value.trim()) {
            return "Въведете заглавие.";
        }
        if (!form.elements.description.value.trim()) {
            return "Въведете описание.";
        }
        if (!form.elements.district.value) {
            return "Изберете квартал.";
        }
        if (!form.elements.address.value.trim()) {
            return "Въведете адрес.";
        }
        if (!form.elements.availableFrom.value || !form.elements.availableTo.value) {
            return "Въведете начална и крайна дата.";
        }
        if (form.elements.availableFrom.value > form.elements.availableTo.value) {
            return "Началната дата трябва да е преди или равна на крайната дата.";
        }
        if (!form.elements.phone.value.trim()) {
            return "Въведете телефон.";
        }
        const pricingType = form.elements.pricingType.value;
        const hourly = numberOrNull(form.elements.pricePerHour.value);
        const daily = numberOrNull(form.elements.pricePerDay?.value);
        if (pricingType === "HOURLY" && !(hourly > 0)) {
            return "Попълнете положителна цена на час.";
        }
        if (pricingType === "DAILY" && !(daily > 0)) {
            return "Попълнете положителна цена на ден.";
        }
        if (pricingType === "HOURLY_AND_DAILY" && (!(hourly > 0) || !(daily > 0))) {
            return "Попълнете положителна цена на час и цена на ден.";
        }
        return "";
    }

    function validateGeometryClientSide(geometryGeoJson) {
        if (!geometryGeoJson) {
            return "Очертайте границите на паркомястото върху картата.";
        }
        try {
            const root = JSON.parse(geometryGeoJson);
            const geometry = root.type === "Feature" ? root.geometry : root;
            if (!geometry || geometry.type !== "Polygon" || !Array.isArray(geometry.coordinates)) {
                return "Границите трябва да са GeoJSON Polygon.";
            }
            const ring = geometry.coordinates[0] || [];
            const points = removeClosingCoordinate(ring);
            if (points.length < 3) {
                return "Границите трябва да имат поне 3 точки.";
            }
            for (const coordinate of points) {
                const longitude = Number(coordinate[0]);
                const latitude = Number(coordinate[1]);
                if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
                    return "GeoJSON координатите трябва да са числа.";
                }
                if (!isInsideSofia(latitude, longitude)) {
                    return "Всички точки трябва да са в границите на София.";
                }
            }
            return "";
        } catch (error) {
            return "GeoJSON данните не са валиден JSON.";
        }
    }

    function geometryFromDrawingPoints() {
        const coordinates = state.drawing.points.map((point) => [
            Number(point.longitude.toFixed(6)),
            Number(point.latitude.toFixed(6))
        ]);
        coordinates.push([...coordinates[0]]);
        return JSON.stringify({
            type: "Polygon",
            coordinates: [coordinates]
        });
    }

    function drawTemporaryGeometry(geometryGeoJson) {
        clearDrawingLayer();
        try {
            const layer = L.geoJSON(JSON.parse(geometryGeoJson), {style: drawingStyle()}).addTo(state.drawingLayer);
            const bounds = layer.getBounds();
            if (bounds.isValid()) {
                state.map.fitBounds(bounds, {padding: [44, 44], maxZoom: 18});
            }
        } catch (error) {
            state.drawingLayer.clearLayers();
        }
    }

    function clearDrawingLayer() {
        if (state.drawingLayer) {
            state.drawingLayer.clearLayers();
        }
    }

    function drawingStyle() {
        return {
            color: "#14543f",
            weight: 3,
            fillColor: "#1f7a5b",
            fillOpacity: 0.24,
            dashArray: "7 5"
        };
    }

    function isInsideSofia(latitude, longitude) {
        return latitude >= SOFIA_BOUNDS.minLatitude
            && latitude <= SOFIA_BOUNDS.maxLatitude
            && longitude >= SOFIA_BOUNDS.minLongitude
            && longitude <= SOFIA_BOUNDS.maxLongitude;
    }

    function clamp(value, min, max) {
        return Math.min(max, Math.max(min, value));
    }

    function removeClosingCoordinate(points) {
        if (!Array.isArray(points) || points.length < 2) {
            return Array.isArray(points) ? points : [];
        }
        const first = points[0];
        const last = points[points.length - 1];
        if (Array.isArray(first)
                && Array.isArray(last)
                && Number(first[0]) === Number(last[0])
                && Number(first[1]) === Number(last[1])) {
            return points.slice(0, -1);
        }
        return points;
    }

    function selectedListing() {
        if (!state.selectedId) {
            return null;
        }
        return state.listings.find((listing) => String(listing.id) === state.selectedId) || null;
    }

    function upsertListing(listing) {
        if (!listing || listing.id == null) {
            return;
        }
        const listingId = String(listing.id);
        const existingIndex = state.listings.findIndex((item) => String(item.id) === listingId);
        if (existingIndex >= 0) {
            state.listings.splice(existingIndex, 1, listing);
        } else {
            state.listings = [listing, ...state.listings];
        }
    }

    function closePreviewPanel() {
        const panel = document.querySelector("[data-preview-panel]");
        if (panel) {
            panel.classList.remove("has-selection", "is-open");
        }
    }

    function showCreateError(message) {
        const box = document.querySelector("[data-create-error]");
        if (box) {
            box.textContent = message || "Проверете попълнените полета.";
            box.hidden = false;
        }
    }

    function clearCreateError() {
        const box = document.querySelector("[data-create-error]");
        if (box) {
            box.textContent = "";
            box.hidden = true;
        }
    }

    function csrfHeaders() {
        const headers = {
            "Accept": "application/json",
            "X-Requested-With": "XMLHttpRequest"
        };
        if (!csrfToken) {
            return headers;
        }
        headers["X-CSRF-TOKEN"] = csrfToken;
        return headers;
    }

    function parseJsonResponse(response) {
        return response.text().then((text) => {
            const body = text ? parseResponseBody(text) : {};
            if (response.ok) {
                return body;
            }
            return Promise.reject(body);
        });
    }

    function parseResponseBody(text) {
        try {
            return JSON.parse(text);
        } catch (error) {
            return {
                message: "Сървърът върна неочакван отговор. Обновете страницата и опитайте отново."
            };
        }
    }

    function errorMessageFromResponse(error) {
        if (!error) {
            return "Данните не могат да бъдат записани.";
        }
        if (error.name === "TypeError" || String(error.message || "").includes("NetworkError")) {
            return "Няма връзка със сървъра. Проверете дали ParkNet работи и опитайте отново.";
        }
        if (error.message) {
            return error.message;
        }
        if (error.fieldErrors) {
            return Object.values(error.fieldErrors)[0] || "Проверете попълнените полета.";
        }
        return "Данните не могат да бъдат записани.";
    }

    function showToast(message, actionLabel, actionUrl) {
        const toast = document.querySelector("[data-map-toast]");
        if (!toast) {
            return;
        }
        toast.innerHTML = "";
        const text = document.createElement("span");
        text.textContent = message;
        toast.appendChild(text);
        if (actionLabel && actionUrl) {
            const link = document.createElement("a");
            link.href = actionUrl;
            link.textContent = actionLabel;
            toast.appendChild(link);
        }
        toast.hidden = false;
        window.clearTimeout(toast.dataset.timeoutId);
        const timeoutId = window.setTimeout(() => {
            toast.hidden = true;
        }, actionUrl ? 5200 : 3400);
        toast.dataset.timeoutId = String(timeoutId);
    }

    function bindOnboardingHint() {
        const hint = document.querySelector("[data-onboarding-hint]");
        const closeButton = document.querySelector("[data-onboarding-close]");
        if (!hint) {
            return;
        }
        if (mapPreference("parknetMapHintDismissed") === "true") {
            hint.hidden = true;
            return;
        }
        if (closeButton) {
            closeButton.addEventListener("click", dismissOnboardingHint);
        }
    }

    function dismissOnboardingHint() {
        const hint = document.querySelector("[data-onboarding-hint]");
        if (hint) {
            hint.hidden = true;
        }
        setMapPreference("parknetMapHintDismissed", "true");
    }

    function mapPreference(key) {
        try {
            return window.sessionStorage.getItem(key);
        } catch (error) {
            return null;
        }
    }

    function setMapPreference(key, value) {
        try {
            window.sessionStorage.setItem(key, value);
        } catch (error) {
            // The hint can still be dismissed for the current page without storage.
        }
    }

    function bindEmptyActions() {
        const clearFiltersButton = document.querySelector("[data-clear-filters]");
        const showAllButton = document.querySelector("[data-show-all-districts]");
        if (clearFiltersButton) {
            clearFiltersButton.addEventListener("click", () => {
                clearAllFilters();
                showToast("Филтрите са изчистени.");
            });
        }
        if (showAllButton) {
            showAllButton.addEventListener("click", () => {
                selectNeighborhood("", {updateUrl: true});
                showToast("Показани са всички квартали.");
            });
        }
    }

    function clearAllFilters() {
        const form = document.querySelector("[data-map-filter-form]");
        if (form) {
            form.querySelectorAll("input, select").forEach((control) => {
                control.value = "";
            });
        }
        state.activeNeighborhoodCode = "";
        state.selectedId = null;
        state.hoveredId = null;
        syncNeighborhoodUi();
        fetchMapListings({fit: true, updateUrl: true, focusNeighborhood: ""});
    }

    function initializeResponsiveDetails() {
        if (window.matchMedia("(max-width: 840px)").matches) {
            const legend = document.querySelector("[data-map-legend]");
            const results = document.querySelector("[data-mini-results-card]");
            if (legend) {
                legend.open = false;
            }
            if (results) {
                results.open = false;
            }
        }
    }

    function bindMapTools() {
        const fitResultsButton = document.querySelector("[data-map-fit-results]");
        const locateButton = document.querySelector("[data-map-locate]");
        const clearButton = document.querySelector("[data-map-clear]");
        let userMarker = null;

        if (fitResultsButton) {
            fitResultsButton.addEventListener("click", () => {
                fitMapToListings();
                if (!state.listings.length) {
                    showToast("Няма резултати за показване.");
                }
            });
        }
        if (clearButton) {
            clearButton.addEventListener("click", clearSelection);
        }
        if (locateButton) {
            locateButton.addEventListener("click", () => {
                if (!navigator.geolocation) {
                    showToast("Браузърът не поддържа местоположение.");
                    return;
                }

                locateButton.disabled = true;
                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        const latLng = [position.coords.latitude, position.coords.longitude];
                        if (!isInsideSofia(latLng[0], latLng[1])) {
                            showToast("Местоположението е извън зоната на ParkNet.");
                            locateButton.disabled = false;
                            return;
                        }
                        if (userMarker) {
                            userMarker.setLatLng(latLng);
                        } else {
                            userMarker = L.circleMarker(latLng, {
                                radius: 7,
                                color: "#14543f",
                                weight: 3,
                                fillColor: "#ffffff",
                                fillOpacity: 1
                            }).addTo(state.map);
                        }
                        state.map.flyTo(latLng, 16, {duration: 0.7});
                        locateButton.disabled = false;
                    },
                    () => {
                        showToast("Не успяхме да намерим местоположението.");
                        locateButton.disabled = false;
                    },
                    {enableHighAccuracy: true, timeout: 8000, maximumAge: 60000}
                );
            });
        }
    }

    function bindPreviewClose() {
        const closeButton = document.querySelector("[data-preview-close]");
        const backButton = document.querySelector("[data-preview-back]");
        if (closeButton) {
            closeButton.addEventListener("click", clearSelection);
        }
        if (backButton) {
            backButton.addEventListener("click", returnToPreviewResults);
        }
    }

    function fetchMapListings(options) {
        const form = document.querySelector("[data-map-filter-form]");
        const query = form ? new URLSearchParams(new FormData(form)) : new URLSearchParams();
        for (const [key, value] of Array.from(query.entries())) {
            if (value === "") {
                query.delete(key);
            }
        }

        if (options && options.updateUrl) {
            const path = query.toString() ? `/listings?${query.toString()}` : "/listings";
            window.history.replaceState({}, "", path);
        }

        if (state.fetchController) {
            state.fetchController.abort();
        }
        state.fetchController = new AbortController();

        const url = query.toString() ? `/api/listings/map?${query.toString()}` : "/api/listings/map";
        fetch(url, {
            credentials: "same-origin",
            headers: {
                "Accept": "application/json",
                "X-Requested-With": "XMLHttpRequest"
            },
            signal: state.fetchController.signal
        })
            .then(parseJsonResponse)
            .then((listings) => {
                state.listings = Array.isArray(listings) ? listings : [];
                if (options && options.includeListing) {
                    upsertListing(options.includeListing);
                }
                if (state.selectedId && !state.listings.some((listing) => String(listing.id) === state.selectedId)) {
                    clearSelection();
                }
                renderPreviewList();
                renderMapListings({fit: Boolean(options && options.fit)});
                updateResultCount();
                syncNeighborhoodUi();
                if (options && options.focusNeighborhood) {
                    showPreviewListPanel();
                    if (options.focusNeighborhood) {
                        focusNeighborhood(options.focusNeighborhood);
                    }
                }
                if (options && options.selectId) {
                    selectListing(String(options.selectId), {pan: true, zoom: options.zoom || 17});
                }
            })
            .catch((error) => {
                if (error && error.name === "AbortError") {
                    return;
                }
                showToast(errorMessageFromResponse(error));
                renderMapListings({fit: Boolean(options && options.fit)});
            });
    }

    function renderMapListings(options) {
        const zoom = state.map.getZoom();
        const showDots = zoom < 15;
        const showBoundaries = zoom >= 15;
        const showLabels = zoom >= 15;

        state.dotLayer.clearLayers();
        state.polygonLayer.clearLayers();
        state.labelLayer.clearLayers();
        state.layersById.clear();

        const orderedListings = [...state.listings].sort((left, right) => {
            const leftId = String(left.id);
            const rightId = String(right.id);
            if (leftId === state.selectedId || leftId === state.hoveredId) {
                return 1;
            }
            if (rightId === state.selectedId || rightId === state.hoveredId) {
                return -1;
            }
            return 0;
        });

        orderedListings.forEach((listing) => {
            const center = centerFor(listing);
            if (!center) {
                return;
            }

            const listingId = String(listing.id);
            const selected = state.selectedId === listingId;
            const hovered = state.hoveredId === listingId;
            const registryEntry = {listing: listing, dot: null, shape: null, label: null};

            if (showDots) {
                registryEntry.dot = createDotLayer(listing, center, selected, hovered).addTo(state.dotLayer);
            }

            if (showBoundaries) {
                registryEntry.shape = createBoundaryLayer(listing, center, selected, hovered);
                if (registryEntry.shape) {
                    registryEntry.shape.addTo(state.polygonLayer);
                }
            }

            if (showLabels) {
                registryEntry.label = createPriceLabel(listing, center, selected, zoom).addTo(state.labelLayer);
            }

            if (selected || hovered) {
                bringListingLayersToFront(registryEntry);
            }

            state.layersById.set(listingId, registryEntry);
        });

        updateLegend();
        applySelectedPreviewState();
        if (options && options.fit) {
            fitMapToListings();
        }
    }

    function createDotLayer(listing, center, selected, hovered) {
        const color = colorForListing(listing);
        const dot = L.circleMarker(center, {
            radius: selected ? 8 : hovered ? 7 : 5.5,
            color: "#ffffff",
            weight: selected ? 3 : 2,
            fillColor: color,
            fillOpacity: selected || hovered ? 1 : 0.88,
            opacity: 1,
            className: `adaptive-map-dot${selected || hovered ? " is-highlighted" : ""}`
        });
        bindLayerInteraction(dot, listing);
        return dot;
    }

    function createBoundaryLayer(listing, center, selected, hovered) {
        const style = boundaryStyle(listing, selected, hovered);
        let layer = null;
        if (listing.geometryGeoJson) {
            try {
                layer = L.geoJSON(JSON.parse(listing.geometryGeoJson), {style: style});
            } catch (error) {
                layer = null;
            }
        }
        if (!layer) {
            layer = L.circle(center, {
                ...style,
                radius: 8
            });
        }
        bindLayerInteraction(layer, listing);
        return layer;
    }

    function createPriceLabel(listing, center, selected, zoom) {
        const prominent = zoom >= 17;
        const label = L.marker(center, {
            interactive: true,
            icon: L.divIcon({
                className: "parking-price-label-anchor",
                html: `<span class="parking-price-label${prominent ? " is-prominent" : ""}${selected ? " is-selected" : ""}">${escapeHtml(rateLabelFor(listing))}</span>`,
                iconSize: [1, 1],
                iconAnchor: [0, 0]
            })
        });
        bindLayerInteraction(label, listing);
        return label;
    }

    function bindLayerInteraction(layer, listing) {
        const listingId = String(listing.id);
        layer.on("mouseover", () => {
            if (!state.drawing.active) {
                setHover(listingId);
            }
        });
        layer.on("mouseout", () => {
            if (!state.drawing.active) {
                setHover(null);
            }
        });
        layer.on("click", () => {
            if (!state.drawing.active) {
                selectListing(listingId, {pan: false});
            }
        });
    }

    function boundaryStyle(listing, selected, hovered) {
        const color = colorForListing(listing);
        const priceModeDimmed = state.colorMode === "price" && isUnavailableForPriceMode(listing);
        const selectedAtDetailZoom = selected && state.map.getZoom() >= 17;
        return {
            color: priceModeDimmed ? "#69717a" : color,
            weight: selectedAtDetailZoom ? 5 : selected ? 4 : hovered ? 3 : 2,
            opacity: priceModeDimmed ? 0.72 : 1,
            fillColor: color,
            fillOpacity: priceModeDimmed ? 0.12 : selected ? 0.42 : hovered ? 0.34 : 0.24,
            dashArray: priceModeDimmed ? "5 5" : null,
            className: `adaptive-boundary${selected ? " is-selected" : ""}${hovered ? " is-hovered" : ""}`
        };
    }

    function bringListingLayersToFront(registryEntry) {
        [registryEntry.dot, registryEntry.shape, registryEntry.label].forEach((layer) => {
            if (layer && typeof layer.bringToFront === "function") {
                layer.bringToFront();
            }
        });
    }

    function fitMapToListings() {
        const bounds = L.latLngBounds([]);
        state.listings.forEach((listing) => {
            const center = centerFor(listing);
            if (center) {
                bounds.extend(center);
            }
            const geometryBounds = boundsForGeometry(listing);
            if (geometryBounds && geometryBounds.isValid()) {
                bounds.extend(geometryBounds);
            }
        });

        if (bounds.isValid()) {
            state.map.fitBounds(bounds, fitOptions());
        } else {
            state.map.setView(sofiaCenter, 12);
        }
    }

    function fitOptions() {
        if (window.matchMedia("(max-width: 840px)").matches) {
            return {paddingTopLeft: [36, 130], paddingBottomRight: [36, 210], maxZoom: 15};
        }
        return {paddingTopLeft: [250, 120], paddingBottomRight: [420, 90], maxZoom: 15};
    }

    function selectListing(listingId, options) {
        state.selectedId = String(listingId);
        const selected = state.listings.find((listing) => String(listing.id) === state.selectedId);
        if (!selected) {
            return;
        }
        updatePreviewPanel(selected);
        renderMapListings();

        if (options && options.pan) {
            const center = centerFor(selected);
            if (center) {
                state.map.flyTo(center, Math.max(state.map.getZoom(), options.zoom || 16), {duration: 0.55});
            }
        }
    }

    function clearSelection() {
        state.selectedId = null;
        state.hoveredId = null;
        const panel = document.querySelector("[data-preview-panel]");
        const emptyState = document.querySelector("[data-preview-empty]");
        const content = document.querySelector("[data-preview-content]");
        const title = document.querySelector("[data-preview-title]");
        const backButton = document.querySelector("[data-preview-back]");
        const editBoundaryButton = document.querySelector("[data-preview-edit-boundary]");
        const ownerNote = document.querySelector("[data-preview-owner-note]");
        if (panel) {
            panel.classList.remove("has-selection", "is-open");
        }
        if (emptyState) {
            emptyState.hidden = false;
        }
        if (content) {
            content.hidden = true;
        }
        if (title) {
            title.textContent = "Избери място от картата";
        }
        if (backButton) {
            backButton.hidden = true;
        }
        if (editBoundaryButton) {
            editBoundaryButton.hidden = true;
        }
        if (ownerNote) {
            ownerNote.hidden = true;
        }
        renderMapListings();
    }

    function returnToPreviewResults() {
        state.selectedId = null;
        state.hoveredId = null;
        showPreviewListPanel();
        renderPreviewList();
        renderMapListings();
        updateResultCount();
    }

    function showPreviewListPanel() {
        const panel = document.querySelector("[data-preview-panel]");
        const emptyState = document.querySelector("[data-preview-empty]");
        const content = document.querySelector("[data-preview-content]");
        const title = document.querySelector("[data-preview-title]");
        const backButton = document.querySelector("[data-preview-back]");
        const editBoundaryButton = document.querySelector("[data-preview-edit-boundary]");
        const ownerNote = document.querySelector("[data-preview-owner-note]");
        if (panel) {
            panel.classList.add("is-open");
            panel.classList.remove("has-selection");
        }
        if (emptyState) {
            emptyState.hidden = false;
        }
        if (content) {
            content.hidden = true;
        }
        if (title) {
            title.textContent = state.activeNeighborhoodCode
                ? `Места в ${currentNeighborhoodName()}`
                : "Избери място от картата";
        }
        if (backButton) {
            backButton.hidden = true;
        }
        if (editBoundaryButton) {
            editBoundaryButton.hidden = true;
        }
        if (ownerNote) {
            ownerNote.hidden = true;
        }
    }

    function setHover(listingId) {
        const nextHoveredId = listingId == null ? null : String(listingId);
        if (state.hoveredId === nextHoveredId) {
            return;
        }
        state.hoveredId = nextHoveredId;
        renderMapListings();
    }

    function renderPreviewList() {
        const list = document.querySelector("[data-preview-list]");
        const miniList = document.querySelector("[data-mini-results-list]");
        if (list) {
            list.innerHTML = state.listings.map((listing) => listingListButtonHtml(listing, "preview-list-button")).join("");
            bindListingListButtons(list);
        }
        if (miniList) {
            miniList.innerHTML = state.listings.map((listing) => listingListButtonHtml(listing, "mini-result-card")).join("");
            bindListingListButtons(miniList);
        }
        updateResultCount();
    }

    function listingListButtonHtml(listing, className) {
        return `
            <button type="button" class="${className}" data-select-listing-id="${escapeHtml(listing.id)}">
                <span>
                    <strong>${escapeHtml(listing.title)}</strong>
                    <small>${escapeHtml(listing.districtName || "")} · ${escapeHtml(listing.address || "")}</small>
                </span>
                <b>${escapeHtml(rateLabelFor(listing))}</b>
            </button>
        `;
    }

    function bindListingListButtons(container) {
        container.querySelectorAll("[data-select-listing-id]").forEach((button) => {
            const listingId = String(button.dataset.selectListingId);
            button.addEventListener("click", () => {
                closeUtilityPanels();
                selectListing(listingId, {pan: true, zoom: 17});
            });
            button.addEventListener("mouseenter", () => setHover(listingId));
            button.addEventListener("mouseleave", () => setHover(null));
        });
    }

    function updatePreviewPanel(listing) {
        const panel = document.querySelector("[data-preview-panel]");
        const emptyState = document.querySelector("[data-preview-empty]");
        const content = document.querySelector("[data-preview-content]");
        const backButton = document.querySelector("[data-preview-back]");
        if (!panel || !content) {
            return;
        }

        panel.classList.add("has-selection", "is-open");
        if (emptyState) {
            emptyState.hidden = true;
        }
        content.hidden = false;
        if (backButton) {
            backButton.hidden = false;
        }

        setText("[data-preview-title]", listing.title);
        setText("[data-preview-status]", listing.statusLabel || "Свободно");
        setText("[data-preview-price]", rateLabelFor(listing));
        setText("[data-preview-description]", listing.shortDescription || "");
        setText("[data-preview-district]", listing.districtName || "");
        setText("[data-preview-address]", listing.address || "");
        setText("[data-preview-from]", formatDate(listing.availableFrom));
        setText("[data-preview-to]", formatDate(listing.availableTo));

        const statusPill = document.querySelector("[data-preview-status]");
        if (statusPill) {
            statusPill.style.background = availabilityColorFor(listing);
        }

        const ownerNote = document.querySelector("[data-preview-owner-note]");
        const detailsLink = document.querySelector("[data-preview-details]");
        const reserveLink = document.querySelector("[data-preview-reserve]");
        const editLink = document.querySelector("[data-preview-edit]");
        const editBoundaryButton = document.querySelector("[data-preview-edit-boundary]");
        if (ownerNote) {
            ownerNote.hidden = !listing.ownedByCurrentUser;
        }
        if (detailsLink) {
            detailsLink.href = listing.detailsUrl || `/listings/${listing.id}`;
        }
        if (reserveLink) {
            reserveLink.href = listing.detailsUrl || `/listings/${listing.id}`;
            reserveLink.hidden = Boolean(listing.ownedByCurrentUser);
        }
        if (editBoundaryButton) {
            editBoundaryButton.hidden = !listing.ownedByCurrentUser;
        }
        if (editLink) {
            editLink.href = listing.editUrl || `/listings/${listing.id}/edit`;
            editLink.hidden = !listing.ownedByCurrentUser;
        }

        updatePreviewImage(listing);
        updatePreviewPriceDetails(listing);
        applySelectedPreviewState();
    }

    function updatePreviewPriceDetails(listing) {
        const wrapper = document.querySelector("[data-preview-price-details]");
        const hourlyElement = document.querySelector("[data-preview-hourly-detail]");
        const dailyElement = document.querySelector("[data-preview-daily-detail]");
        if (!wrapper || !hourlyElement || !dailyElement) {
            return;
        }

        const hourly = numberOrNull(listing.pricePerHour);
        const daily = numberOrNull(listing.pricePerDay);
        hourlyElement.textContent = hourly == null ? "" : `${formatMoney(hourly)}€ на час`;
        dailyElement.textContent = daily == null ? "" : `${formatMoney(daily)}€ дневна цена`;
        wrapper.hidden = hourly == null && daily == null;
    }

    function updatePreviewForSelected() {
        const selected = state.listings.find((listing) => String(listing.id) === state.selectedId);
        if (selected) {
            updatePreviewPanel(selected);
        }
    }

    function applySelectedPreviewState() {
        document.querySelectorAll("[data-select-listing-id]").forEach((button) => {
            button.classList.toggle("is-active", String(button.dataset.selectListingId) === state.selectedId);
            button.classList.toggle("is-hovered", String(button.dataset.selectListingId) === state.hoveredId);
        });
    }

    function updatePreviewImage(listing) {
        const imageWrap = document.querySelector("[data-preview-image-wrap]");
        const image = document.querySelector("[data-preview-image]");
        if (!imageWrap || !image) {
            return;
        }
        if (!listing.imagePath) {
            imageWrap.hidden = true;
            image.removeAttribute("src");
            image.alt = "";
            return;
        }
        image.src = listing.imagePath;
        image.alt = "Снимка на паркомясто";
        imageWrap.hidden = false;
    }

    function updateResultCount() {
        const count = state.listings.length;
        const neighborhoodName = currentNeighborhoodName();
        const countElements = document.querySelectorAll("[data-map-count-number], .map-result-count strong");
        const labelElements = document.querySelectorAll("[data-map-count-label], .map-result-count span");
        const emptyOverlay = document.querySelector(".map-empty-overlay");
        const emptyTitle = document.querySelector(".map-empty-overlay h2");
        const emptyText = document.querySelector(".map-empty-overlay p");
        const emptyPreview = document.querySelector("[data-preview-empty]");
        const previewMessage = document.querySelector("[data-preview-message]");
        const miniCount = document.querySelector("[data-mini-results-count]");
        countElements.forEach((countElement) => {
            countElement.textContent = neighborhoodName ? `Намерени ${count}` : String(count);
        });
        if (miniCount) {
            miniCount.textContent = String(count);
        }
        labelElements.forEach((labelElement) => {
            if (neighborhoodName) {
                labelElement.textContent = `${count === 1 ? "място" : "места"} в ${neighborhoodName}`;
            } else {
                labelElement.textContent = count === 1 ? "намерено място" : "намерени места";
            }
        });
        if (emptyOverlay) {
            emptyOverlay.hidden = count > 0;
        }
        if (emptyTitle) {
            emptyTitle.textContent = neighborhoodName && count === 0
                ? "Все още няма обяви в този квартал."
                : "Няма намерени места по тези филтри.";
        }
        if (emptyText) {
            emptyText.textContent = neighborhoodName && count === 0
                ? "Картата остава върху избрания квартал. Опитай друг квартал или създай първата обява."
                : "Изчисти филтрите или покажи всички квартали, за да продължиш търсенето.";
        }
        if (emptyPreview && count === 0) {
            emptyPreview.hidden = false;
        }
        if (previewMessage) {
            if (count === 0 && neighborhoodName) {
                previewMessage.textContent = "Все още няма обяви в този квартал.";
            } else if (count === 0) {
                previewMessage.textContent = "Няма намерени места за тези филтри.";
            } else {
                previewMessage.textContent = `Намерени ${count} ${count === 1 ? "място" : "места"}${neighborhoodName ? ` в ${neighborhoodName}` : ""}. Натисни място, за да видиш детайли.`;
            }
        }
    }

    function currentNeighborhoodName() {
        if (!state.activeNeighborhoodCode) {
            return "";
        }
        return NEIGHBORHOODS[state.activeNeighborhoodCode]?.name || neighborhoodFromGlobalCenters(state.activeNeighborhoodCode)?.name || "";
    }

    function updateLegend() {
        const legend = document.querySelector("[data-legend-items]");
        const help = document.querySelector("[data-legend-help]");
        if (!legend) {
            return;
        }

        if (state.colorMode === "price") {
            const unit = priceTierUnit();
            legend.innerHTML = `
                <span><i class="legend-swatch" style="background:${COLORS.cheap}"></i>${escapeHtml(unit.cheap)}</span>
                <span><i class="legend-swatch" style="background:${COLORS.medium}"></i>${escapeHtml(unit.medium)}</span>
                <span><i class="legend-swatch" style="background:${COLORS.expensive}"></i>${escapeHtml(unit.expensive)}</span>
                <span><i class="legend-swatch legend-border"></i>Недостъпните са сив контур</span>
            `;
        } else {
            legend.innerHTML = `
                <span><i class="legend-swatch" style="background:${COLORS.available}"></i>Свободно</span>
                <span><i class="legend-swatch" style="background:${COLORS.requested}"></i>Заявено</span>
                <span><i class="legend-swatch" style="background:${COLORS.booked}"></i>Заето</span>
                <span><i class="legend-swatch" style="background:${COLORS.owned}"></i>Моите обяви</span>
                <span><i class="legend-swatch" style="background:${COLORS.inactive}"></i>Неактивно</span>
            `;
        }
        if (help) {
            help.textContent = "2€ = цена на час. Дневните отстъпки са в детайлите.";
        }
    }

    function colorForListing(listing) {
        if (state.colorMode === "price") {
            return priceColorFor(listing);
        }
        return availabilityColorFor(listing);
    }

    function availabilityColorFor(listing) {
        if (listing.ownedByCurrentUser) {
            return COLORS.owned;
        }
        const status = String(listing.availabilityStatus || listing.colorCategory || "").toUpperCase();
        if (status.includes("REQUESTED")) {
            return COLORS.requested;
        }
        if (status.includes("BOOKED")) {
            return COLORS.booked;
        }
        if (status.includes("UNAVAILABLE")) {
            return COLORS.unavailable;
        }
        if (status.includes("INACTIVE")) {
            return COLORS.inactive;
        }
        return COLORS.available;
    }

    function priceColorFor(listing) {
        if (listing.ownedByCurrentUser) {
            return COLORS.owned;
        }
        const tier = priceTierFor(listing);
        if (tier === "expensive") {
            return COLORS.expensive;
        }
        if (tier === "medium") {
            return COLORS.medium;
        }
        return COLORS.cheap;
    }

    function priceTierFor(listing) {
        const choice = selectedRateValue(listing);
        if (!choice.value) {
            return "cheap";
        }
        const cheapLimit = 1.5;
        const mediumLimit = 2.5;
        if (choice.value <= cheapLimit) {
            return "cheap";
        }
        if (choice.value <= mediumLimit) {
            return "medium";
        }
        return "expensive";
    }

    function priceTierUnit() {
        return {
            cheap: "Евтино: до 1.50€",
            medium: "Средно: 1.51-2.50€",
            expensive: "Скъпо: над 2.50€"
        };
    }

    function isUnavailableForPriceMode(listing) {
        const status = String(listing.availabilityStatus || "").toUpperCase();
        return status.includes("BOOKED") || status.includes("UNAVAILABLE") || status.includes("REQUESTED") || status.includes("INACTIVE");
    }

    function rateLabelFor(listing) {
        const hourly = numberOrNull(listing.pricePerHour);
        return hourly == null ? "—" : `${formatMoney(hourly)}€`;
    }

    function selectedRateValue(listing) {
        const hourly = numberOrNull(listing.pricePerHour);
        return {value: hourly, unit: "hourly"};
    }

    function centerFor(listing) {
        const latitude = Number(listing.centerLatitude);
        const longitude = Number(listing.centerLongitude);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
            return null;
        }
        return [latitude, longitude];
    }

    function boundsForGeometry(listing) {
        if (!listing.geometryGeoJson) {
            return null;
        }
        try {
            return L.geoJSON(JSON.parse(listing.geometryGeoJson)).getBounds();
        } catch (error) {
            return null;
        }
    }

    function setText(selector, value) {
        const element = document.querySelector(selector);
        if (element) {
            element.textContent = value || "";
        }
    }

    function formatDate(value) {
        if (!value) {
            return "";
        }
        const parts = String(value).split("-");
        if (parts.length !== 3) {
            return String(value);
        }
        return `${parts[2]}.${parts[1]}.${parts[0]}`;
    }

    function formatMoney(value) {
        if (value == null) {
            return "";
        }
        return Number(value).toLocaleString("bg-BG", {
            minimumFractionDigits: Number.isInteger(Number(value)) ? 0 : 2,
            maximumFractionDigits: 2
        });
    }

    function numberOrNull(value) {
        if (value == null || String(value).trim() === "") {
            return null;
        }
        const number = Number(value);
        return Number.isFinite(number) ? number : null;
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#039;");
    }
})();
