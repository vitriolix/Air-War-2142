package com.vitriolix.airwar2142.widgets

import korlibs.korge.input.onClick
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.Size

// ── SPIKE (spike/korge-ui-widgets) ───────────────────────────────────────────
// A reusable compound widget: a UISlider flanked by [-] / [+] buttons. Demonstrates
// that KorGE widgets compose like any View — this wraps the stock UISlider WITHOUT
// reimplementing its logic: the [-]/[+] buttons just nudge `slider.value`, and the
// slider's own setter does the clamp + step-snap + onChange firing
// (`value.clamp(min,max).nearestAlignedTo(step)`). The buttons auto-disable at the
// range ends. Consumers bind one `onChange`. Usable as `uiSteppedSlider(...)`.
class UISteppedSlider(
    value: Double,
    min: Double,
    max: Double,
    val step: Double,
    decimalPlaces: Int? = 1,
    size: Size = Size(800.0, 56.0),
    buttonWidth: Double = 80.0,
    gap: Double = 12.0,
    skin: (UIButton.() -> Unit)? = null,
) : UIView(size) {

    private val slider: UISlider
    private val minusBtn: UIButton
    private val plusBtn: UIButton

    /** The slider's own signal — fires for both drag and the [-]/[+] buttons. */
    val onChange get() = slider.onChange

    var value: Double
        get() = slider.value
        set(v) { slider.value = v }

    /** Input-source-agnostic nudges — wired to the [-]/[+] clicks AND the scene's LEFT/RIGHT keys. */
    fun decrement() { slider.value -= step }
    fun increment() { slider.value += step }

    init {
        val sliderW = size.width - 2.0 * (buttonWidth + gap)
        // Plain ASCII '-' / '+': the bespoke Chakra Petch subset doesn't cover the
        // U+2212 MINUS SIGN (renders blank), same gap as the ✈ note elsewhere in the code.
        minusBtn = uiButton(label = "-", size = Size(buttonWidth, size.height)) {
            skin?.invoke(this)
            onClick { decrement() }               // reuses clamp/snap/onChange
        }.position(0.0, 0.0)
        slider = uiSlider(value, min, max, step, decimalPlaces, size = Size(sliderW, size.height))
            .position(buttonWidth + gap, 0.0)
        plusBtn = uiButton(label = "+", size = Size(buttonWidth, size.height)) {
            skin?.invoke(this)
            onClick { increment() }
        }.position(size.width - buttonWidth, 0.0)

        // Smart ends: at a bound, block the button AND visibly fade it (the stock disabled
        // dim is too subtle on a dark skin, so drive `alpha` for an unambiguous cue).
        slider.onChange { refreshEnds() }
        refreshEnds()
    }

    private fun refreshEnds() {
        minusBtn.enabled = slider.value > slider.min
        plusBtn.enabled = slider.value < slider.max
        minusBtn.alpha = if (minusBtn.enabled) 1.0 else 0.3
        plusBtn.alpha = if (plusBtn.enabled) 1.0 else 0.3
    }
}

/** Builder so it reads like the stock widgets: `uiSteppedSlider(...) { onChange { … } }`. */
fun Container.uiSteppedSlider(
    value: Double,
    min: Double,
    max: Double,
    step: Double,
    decimalPlaces: Int? = 1,
    size: Size = Size(800.0, 56.0),
    skin: (UIButton.() -> Unit)? = null,
    block: UISteppedSlider.() -> Unit = {},
): UISteppedSlider = UISteppedSlider(value, min, max, step, decimalPlaces, size, skin = skin).addTo(this).apply(block)
