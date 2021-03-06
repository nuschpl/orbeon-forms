/**
 * Copyright (C) 2017 Orbeon, Inc.
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
package org.orbeon.builder

import autowire._
import enumeratum.EnumEntry.Hyphencase
import enumeratum._
import org.orbeon.builder.BlockCache.Block
import org.orbeon.builder.rpc.{FormBuilderClient, FormBuilderRpcApi}
import org.orbeon.datatypes.Direction
import org.orbeon.jquery.Offset
import org.orbeon.oxf.util.CoreUtils.asUnit
import org.orbeon.oxf.util.StringUtils._
import org.orbeon.xforms._
import org.scalajs.jquery.JQuery

import scala.concurrent.ExecutionContext.Implicits.global

object SectionGridEditor {

  lazy val sectionGridEditorContainer               = $(".fb-section-grid-editor")
  lazy val rowEditorContainer                       = $(".fb-row-editor")

  var currentSectionGridBodyOpt : Option[Block] = None
  var currentRowPosOpt          : Option[Int]   = None

  sealed trait GridSectionEditor extends EnumEntry with Hyphencase
  object GridSectionEditor extends Enum[GridSectionEditor] {
    val values = findValues
    case object SectionDelete      extends GridSectionEditor
    case object SectionEditDetails extends GridSectionEditor
    case object SectionEditHelp    extends GridSectionEditor
    case object SectionMoveUp      extends GridSectionEditor
    case object SectionMoveDown    extends GridSectionEditor
    case object SectionMoveRight   extends GridSectionEditor
    case object SectionMoveLeft    extends GridSectionEditor
    case object SectionEditors     extends GridSectionEditor
    case object GridEditDetails    extends GridSectionEditor
    case object GridDelete         extends GridSectionEditor

    // What class, if any, must be present on the corresponding container to the the editor to be enabled
    def enableClass(editor: GridSectionEditor): Option[String] =
      editor match {
        case SectionMoveUp    ⇒ Some("fb-can-move-up")
        case SectionMoveDown  ⇒ Some("fb-can-move-down")
        case SectionMoveRight ⇒ Some("fb-can-move-right")
        case SectionMoveLeft  ⇒ Some("fb-can-move-left")
        case _                ⇒ None
      }
  }

  sealed case class RowEditor(selector: String)
  val RowInsertAbove = RowEditor(".icon-chevron-up")
  val RowDelete      = RowEditor(".icon-minus-sign")
  val RowInsertBelow = RowEditor(".icon-chevron-down")
  val RowEditors     = List(RowInsertAbove, RowDelete, RowInsertBelow)

  // Position editor when block becomes current
  Position.currentContainerChanged(
    containerCache = BlockCache.sectionGridBodyCache,
    wasCurrent = (sectionGridBody: Block) ⇒ {
      if (sectionGridBody.el.is(BlockCache.GridBodySelector))
        sectionGridBody.el.parent.removeClass("fb-hover")
    },
    becomesCurrent = (sectionGridBody: Block) ⇒ {
      currentSectionGridBodyOpt = Some(sectionGridBody)

      // Position the editor
      sectionGridEditorContainer.show()
      Offset.offset(
        sectionGridEditorContainer,
        Offset(
          // Use `.fr-body` left rather than the section left to account for sub-sections indentation
          left = Offset($(".fr-body")).left - sectionGridEditorContainer.outerWidth(),
          top  = sectionGridBody.top - Position.scrollTop()
        )
      )

      // Start by hiding all the icons
      sectionGridEditorContainer.children().hide()

      // Update triggers relevance for section
      if (sectionGridBody.el.is(BlockCache.SectionSelector)) {

        // Edit details and help are always visible
        sectionGridEditorContainer
          .children(".fb-section-edit-details, .fb-section-delete, .fb-section-edit-help")
          .show()

        // Hide/show section move icons
        val container = sectionGridBody.el.children(".fr-section-container")
        Direction.values foreach { direction ⇒
          val relevant = container.hasClass("fb-can-move-" + direction.entryName.toLowerCase)
          val trigger  = sectionGridEditorContainer.children(".fb-section-move-" + direction.entryName.toLowerCase)
          if (relevant) trigger.show()
        }

        // Hide/show delete icon
        val deleteTrigger = sectionGridEditorContainer.children(".delete-section-trigger")
        if (container.is(".fb-can-delete")) deleteTrigger.show()
      }

      // Update triggers relevance for repeated grid only
      if (sectionGridBody.el.is(BlockCache.GridBodySelector)) {

        sectionGridEditorContainer.children(".fb-grid-delete").show()

        if (sectionGridBody.el.closest(".fr-grid").is(".fr-repeat"))
          sectionGridEditorContainer.children(".fb-grid-edit-details").show()

        sectionGridBody.el.parent.addClass("fb-hover")
      }
    }
  )

  // Hide editor when the pointer gets out of the Form Builder main area
  Position.currentContainerChanged(
    containerCache = BlockCache.fbMainCache,
    wasCurrent = (_: Block) ⇒ hideSideEditors(),
    becomesCurrent = (_: Block) ⇒ ( /* NOP */ )
  )

  // Also hide when blocks may have moved, e.g. on Ajax response, say after a row was added,
  // to avoid inappropriately positioned editors
  Position.onOffsetMayHaveChanged(hideSideEditors _)

  def hideSideEditors(): Unit = {
    sectionGridEditorContainer.hide()
    rowEditorContainer.hide()
    currentSectionGridBodyOpt = None
    currentRowPosOpt          = None
  }

  // Position row editor
  Position.onUnderPointerChange {
    withCurrentGridBody((currentGridBody) ⇒ {

      // Get the height of each row track
      val rowsHeight =
        currentGridBody.el
          .css("grid-template-rows")
          .splitTo[List]()
          .map((hPx) ⇒ hPx.substring(0, hPx.indexOf("px")))
          .map(_.toDouble)

      case class TopBottom(top: Double, bottom: Double)

      // For each row track, find its top/bottom
      val rowsTopBottom = {
        val gridBodyTop = currentGridBody.top
        val zero = List(TopBottom(0, gridBodyTop))
        rowsHeight.foldLeft(zero) { (soFar: List[TopBottom], rowHeight: Double) ⇒
          val lastBottom = soFar.last.bottom
          val newTopBottom = TopBottom(lastBottom, lastBottom + rowHeight)
          soFar :+ newTopBottom
        }.drop(1)
      }

      // Find top/bottom of the row track the pointer is on
      val pointerRowTopBottomIndexOpt = {
        val pointerTop = Position.pointerPos.top + Position.scrollTop()
        rowsTopBottom.zipWithIndex.find { case (topBottom, _) ⇒
          topBottom.top <= pointerTop && pointerTop <= topBottom.bottom
        }
      }

      // Find where to position the row editor on the left
      val containerLeft = Offset(gridFromGridBody(currentGridBody)).left

      // Position row editor
      pointerRowTopBottomIndexOpt.foreach((pointerRowTopBottom) ⇒ {
        rowEditorContainer.show()
        rowEditorContainer.children().hide()

        val rowTop    = pointerRowTopBottom._1.top
        val rowBottom = pointerRowTopBottom._1.bottom
        val rowHeight = rowBottom - rowTop
        val rowIndex  = pointerRowTopBottom._2

        def positionElWithClass(selector: String, topOffset: (JQuery) ⇒ Double): Unit = {
          val elem = rowEditorContainer.children(selector)
          elem.show()
          Offset.offset(
            el = elem,
            offset = Offset(
              left = containerLeft,
              top  = topOffset(elem) - Position.scrollTop()
            )
          )
        }

        currentRowPosOpt = Some(rowIndex + 1)
        positionElWithClass(RowInsertAbove.selector, (_) ⇒ rowTop)
        positionElWithClass(RowDelete.selector  , (e) ⇒ rowTop + rowHeight/2 - e.height()/2)
        positionElWithClass(RowInsertBelow.selector, (e) ⇒ rowBottom - e.height())
      })
    })
  }

  def gridFromGridBody(block: Block): JQuery = {
    assert(block.el.is(BlockCache.GridBodySelector))
    block.el.closest(BlockCache.GridSelector)
  }

  def withCurrentGridBody(fn: Block ⇒ Unit): Unit =
    currentSectionGridBodyOpt.foreach((currentSectionGridBody) ⇒
      if (currentSectionGridBody.el.is(BlockCache.GridBodySelector))
        fn(currentSectionGridBody)
    )

  // Register listener on editor icons
  locally {

    GridSectionEditor.values foreach { editor ⇒

      val iconEl = sectionGridEditorContainer.children(s".fb-${editor.entryName}")

      iconEl.on("click.orbeon.builder.section-grid-editor", () ⇒ asUnit {
        currentSectionGridBodyOpt foreach { currentSectionGridBody ⇒

          val isSection = currentSectionGridBody.el.is(BlockCache.SectionSelector)

          val sectionGrid =
            if (isSection)
              currentSectionGridBody.el
            else
              gridFromGridBody(currentSectionGridBody)

          val sectionGridId = sectionGrid.attr("id").get

          import GridSectionEditor._

          val client = FormBuilderClient[FormBuilderRpcApi]

          editor match {
            case SectionDelete      ⇒ client.sectionDelete     (sectionGridId).call()
            case SectionEditDetails ⇒ client.sectionEditDetails(sectionGridId).call()
            case SectionEditHelp    ⇒ client.sectionEditHelp   (sectionGridId).call()
            case SectionMoveUp      ⇒ client.sectionMoveUp     (sectionGridId).call()
            case SectionMoveDown    ⇒ client.sectionMoveDown   (sectionGridId).call()
            case SectionMoveRight   ⇒ client.sectionMoveRight  (sectionGridId).call()
            case SectionMoveLeft    ⇒ client.sectionMoveLeft   (sectionGridId).call()
            case SectionEditors     ⇒ client.sectionEditors    (sectionGridId).call()
            case GridEditDetails    ⇒ client.gridEditDetails   (sectionGridId).call()
            case GridDelete         ⇒ client.gridDelete        (sectionGridId).call()
          }
        }
      })
    }

    RowEditors foreach { rowEditor ⇒
      val iconEl = rowEditorContainer.children(rowEditor.selector)
      iconEl.on("click.orbeon.builder.section-grid-editor", () ⇒ asUnit {
        withCurrentGridBody { currentGridBody ⇒
          currentRowPosOpt foreach { currentRowPos ⇒

            val controlId = gridFromGridBody(currentGridBody).attr("id").get

            val client = FormBuilderClient[FormBuilderRpcApi]

            rowEditor match {
              case RowInsertAbove ⇒ client.rowInsertAbove(controlId, currentRowPos).call()
              case RowDelete      ⇒ client.rowDelete     (controlId, currentRowPos).call()
              case RowInsertBelow ⇒ client.rowInsertBelow(controlId, currentRowPos).call()
            }
          }
        }
      })
    }
  }
}
