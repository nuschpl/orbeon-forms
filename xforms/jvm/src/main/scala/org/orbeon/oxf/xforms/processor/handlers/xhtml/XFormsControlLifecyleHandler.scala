/**
  * Copyright (C) 2010 Orbeon, Inc.
  *
  * This program is free software; you can redistribute it and/or modify it under the terms of the
  * GNU Lesser General Public License as published by the Free Software Foundation; either version
  * 2.1 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  * See the GNU Lesser General Public License for more details.
  *
  * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
  */
package org.orbeon.oxf.xforms.processor.handlers.xhtml

import java.{lang ⇒ jl}

import org.orbeon.oxf.util.CollectionUtils.collectByErasedType
import org.orbeon.oxf.xforms.analysis.controls.StaticLHHASupport
import org.orbeon.oxf.xforms.control.XFormsControl
import org.orbeon.oxf.xforms.processor.handlers.XFormsBaseHandler
import org.orbeon.oxf.xforms.processor.handlers.XFormsBaseHandler.LHHAC
import org.orbeon.oxf.xml.{XMLConstants, XMLReceiverHelper, XMLUtils}
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl
import org.orbeon.oxf.util.CoreUtils._

import scala.xml.SAXException

/**
  * This class is a helper base class which handles the lifecycle of producing markup for a control. The following
  * phases are handled:
  *
  * - Give the handler a chance to do some prep work: `prepareHandler()`
  * - Get custom information: `addCustomClasses()`
  * - Check whether the control wants any output at all: `isMustOutputControl()`
  * - Output label, control, hint, help, and alert in order specified by properties
  *
  * Outputting the control is split into two parts: `handleControlStart()` and `handleControlEnd()`. In most cases, only
  * `handleControlStart()` is used, but container controls will use `handleControlEnd()`.
  */
abstract class XFormsControlLifecyleHandler(
  uri            : String,
  localname      : String,
  qName          : String,
  attributes     : Attributes,
  matched        : AnyRef,
  handlerContext : AnyRef,
  repeating      : Boolean,
  forwarding     : Boolean
) extends
  XFormsBaseHandlerXHTML(
    uri,
    localname,
    qName,
    attributes,
    matched,
    handlerContext,
    repeating,
    forwarding
  ) {

  import Private._

  val isTemplate     = xformsHandlerContext.isTemplate
  val getPrefixedId  = xformsHandlerContext.getPrefixedId(attributes)
  val getEffectiveId = xformsHandlerContext.getEffectiveId(attributes)

  val currentControlOpt: Option[XFormsControl] =
    ! xformsHandlerContext.isTemplate                            option
      containingDocument.getControlByEffectiveId(getEffectiveId) ensuring
      (! _.contains(null))

  def currentControlOrNull = currentControlOpt.orNull

  def staticControlOpt = currentControlOpt map (_.staticControl)

  // By default, controls are enclosed with a <span>
  protected def getContainingElementName = "span"

  protected def getContainingElementQName: String =
    XMLUtils.buildQName(xformsHandlerContext.findXHTMLPrefix, getContainingElementName)

  @throws[SAXException]
  override final def start(): Unit =
    if (isMustOutputControl(currentControlOrNull)) {

      // Open control element, usually `<span>`
      if (isMustOutputContainerElement)
        xformsHandlerContext.getController.getOutput.startElement(
          XMLConstants.XHTML_NAMESPACE_URI,
          getContainingElementName,
          getContainingElementQName,
          getContainerAttributes(uri, localname, attributes)
        )

      // 2012-12-17: Removed nested `<a name="effective-id">` because the enclosing `<span`> for the control has the
      // same id and will be handled first by the browser as per HTML 5. This means the named anchor is actually
      // redundant.

      // Process everything up to and including the control
      for (current ← beforeAfterTokens._1)
        current match {
          case "control" ⇒ handleControlStart()
          case "label"   ⇒ if (hasLocalLabel) handleLabel()
          case "alert"   ⇒ if (hasLocalAlert) handleAlert()
          case "hint"    ⇒ if (hasLocalHint)  handleHint()
          case "help"    ⇒ if (hasLocalHelp)  handleHelp()
        }
    }

  @throws[SAXException]
  override final def end(): Unit =
    if (isMustOutputControl(currentControlOrNull)) {

      // Process everything after the control has been shown
      for (current ← beforeAfterTokens._2)
        current match {
          case "control" ⇒ handleControlEnd()
          case "label"   ⇒ if (hasLocalLabel) handleLabel()
          case "alert"   ⇒ if (hasLocalAlert) handleAlert()
          case "hint"    ⇒ if (hasLocalHint)  handleHint()
          case "help"    ⇒ if (hasLocalHelp)  handleHelp()
        }

      // Close control element, usually `<span>`
      if (isMustOutputContainerElement)
        xformsHandlerContext.getController.getOutput.endElement(
          XMLConstants.XHTML_NAMESPACE_URI,
          getContainingElementName,
          getContainingElementQName
        )
    }

  // May be overridden by subclasses
  protected def isMustOutputControl(control: XFormsControl)                         = true
  protected def isMustOutputContainerElement                                        = true
  protected def addCustomClasses(classes: jl.StringBuilder, control: XFormsControl) = ()
  protected def isDefaultIncremental                                                = false

  @throws[SAXException]
  protected def handleLabel(): Unit =
    handleLabelHintHelpAlert(
      getStaticLHHA(getPrefixedId, LHHAC.LABEL),
      getEffectiveId,
      getForEffectiveId(getEffectiveId),
      LHHAC.LABEL,
      if (XFormsBaseHandler.isStaticReadonly(currentControlOrNull)) "span" else null,
      currentControlOrNull,
      isTemplate,
      false
    )

  @throws[SAXException]
  protected def handleAlert(): Unit =
    if (! XFormsBaseHandler.isStaticReadonly(currentControlOrNull))
      handleLabelHintHelpAlert(
        getStaticLHHA(getPrefixedId, LHHAC.ALERT),
        getEffectiveId,
        getForEffectiveId(getEffectiveId),
        LHHAC.ALERT,
        null,
        currentControlOrNull,
        isTemplate,
        false
      )

  @throws[SAXException]
  protected def handleHint(): Unit =
    if (! XFormsBaseHandler.isStaticReadonly(currentControlOrNull) || containingDocument.staticReadonlyHint)
      handleLabelHintHelpAlert(
        getStaticLHHA(getPrefixedId, LHHAC.HINT),
        getEffectiveId,
        getForEffectiveId(getEffectiveId),
        LHHAC.HINT,
        null,
        currentControlOrNull,
        isTemplate,
        false
      )

  @throws[SAXException]
  protected def handleHelp(): Unit =
    if (! XFormsBaseHandler.isStaticReadonly(currentControlOrNull))
      handleLabelHintHelpAlert(
        getStaticLHHA(getPrefixedId, LHHAC.HELP),
        getEffectiveId,
        getForEffectiveId(getEffectiveId),
        LHHAC.HELP,
        null,
        currentControlOrNull,
        isTemplate,
        false
      )

  // Must be overridden by subclasses
  @throws[SAXException]
  protected def handleControlStart(): Unit

  @throws[SAXException]
  protected def handleControlEnd() = ()

  protected def getEmptyNestedControlAttributesMaybeWithId(
    effectiveId : String,
    control     : XFormsControl,
    addId       : Boolean
  ): AttributesImpl = {
    reusableAttributes.clear()
    val containerAttributes = reusableAttributes
    if (addId)
      containerAttributes.addAttribute(
        "",
        "id",
        "id",
        XMLReceiverHelper.CDATA,
        XFormsBaseHandler.getLHHACId(containingDocument, effectiveId, XFormsBaseHandler.LHHAC_CODES.get(LHHAC.CONTROL))
      )
    containerAttributes
  }

  // Return the effective id of the element to which label/@for, etc. must point to.
  // Default: point to `foo$bar$$c.1-2-3`
  def getForEffectiveId(effectiveId: String): String =
    XFormsBaseHandler.getLHHACId(containingDocument, getEffectiveId, XFormsBaseHandler.LHHAC_CODES.get(LHHAC.CONTROL))

  private object Private {

    val beforeAfterTokens: (List[String], List[String]) =
      staticControlOpt                       flatMap
      collectByErasedType[StaticLHHASupport] flatMap
      (_.beforeAfterTokensOpt)               getOrElse
      xformsHandlerContext.getDocumentOrder

    def hasLocalLabel = hasLocalLHHA("label")
    def hasLocalHint  = hasLocalLHHA("hint")
    def hasLocalHelp  = hasLocalLHHA("help")
    def hasLocalAlert = hasLocalLHHA("alert")

    def hasLocalLHHA(lhhaType: String) =
      containingDocument.getStaticOps.getControlAnalysis(getPrefixedId) match {
        case support: StaticLHHASupport ⇒ support.hasLocal(lhhaType)
        case _                          ⇒ false
      }

    def getContainerAttributes(
      uri        : String,
      localname  : String,
      attributes : Attributes
    ): AttributesImpl = {
      // NOTE: Only reason we do not use the class members directly is to handle boolean xf:input, which delegates
      // its output to xf:select1. Should be improved some day.
      val prefixedId    = getPrefixedId
      val effectiveId   = getEffectiveId
      val xformsControl = currentControlOrNull

      // Get classes
      // Initial classes: `xforms-control`, `xforms-[control name]`, `incremental`, `appearance`, `mediatype`, `xforms-static`
      val classes =
        getInitialClasses(uri, localname, attributes, xformsControl, isDefaultIncremental)

      // All MIP-related classes
      handleMIPClasses(classes, prefixedId, xformsControl)

      // Static classes
      containingDocument.getStaticOps.appendClasses(classes, prefixedId)

      // Dynamic classes added by the control
      addCustomClasses(classes, xformsControl)

      // Get attributes
      val newAttributes = getIdClassXHTMLAttributes(attributes, classes.toString, effectiveId)

      // Add extension attributes in no namespace if possible
      if (xformsControl ne null)
        xformsControl.addExtensionAttributesExceptClassAndAcceptForHandler(newAttributes, "")

      newAttributes
    }
  }
}