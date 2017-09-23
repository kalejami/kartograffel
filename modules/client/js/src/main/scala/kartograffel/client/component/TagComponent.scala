package kartograffel.client.component

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidMount
import japgolly.scalajs.react.vdom.html_<^._
import kartograffel.client.repository.ClientRepository
import kartograffel.shared.model.{Entity, Graffel, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object TagComponent {

  case class State(currentGraffel: Option[Entity[Graffel]],
                   tags: List[Tag],
                   tagInput: String,
                   submittingTag: Boolean)

  class Backend(scope: BackendScope[Unit, State]) {
    def render(state: State): VdomElement =
      <.div(
        tagList(state),
        tagSubmit(state)
      )

    private def tagList(state: State) =
      TagListComponent.component(TagListComponent.Props(state.tags))

    private def tagSubmit(state: State) = {
      def onSubmit(state: State): Callback = {
        val modStateSubmittingTag = scope.modState(_.copy(submittingTag = true))
        val modStateFinishedSubmitting =
          scope.modState(_.copy(tagInput = "", submittingTag = false))
        def loadTagsByGraffel(graffel: Option[Entity[Graffel]]) =
          state.currentGraffel
            .map(entity =>
              ClientRepository.future.findTags(entity.value.position))
            .getOrElse(Nil)
        def saveNewTag() =
          state.currentGraffel
            .map(entity => Tag(state.tagInput, entity.id))
            .map(ClientRepository.future.saveTag)
            .getOrElse(Future.successful(()))

        val io = CallbackTo.future {
          val result = for {
            _ <- saveNewTag()
            loadedTags <- loadTagsByGraffel(state.currentGraffel)
          } yield scope.modState(_.copy(tags = loadedTags))

          result.onComplete {
            case Success(_) => ()
            case Failure(throwable) =>
              throwable.printStackTrace() //Todo render that error happened
          }
          result
        }

        modStateSubmittingTag >> io >> modStateFinishedSubmitting
      }

      def onChange(e: ReactEventFromInput): Callback =
        scope.modState(_.copy(tagInput = e.target.value))

      TagSubmitComponent.component(
        TagSubmitComponent.Props(
          onChange = onChange,
          onSubmit = _ => onSubmit(state),
          inputText = state.tagInput,
          enabled = !state.submittingTag
        )
      )
    }
  }

  val component = ScalaComponent
    .builder[Unit]("TagComponent")
    .initialState(
      State(
        currentGraffel = None,
        tags = Nil,
        tagInput = "",
        submittingTag = false
      ))
    .renderBackend[Backend]
    .componentDidMount(onComponentDidMount)
    .build

  private def onComponentDidMount(
      cdm: ComponentDidMount[Unit, State, Backend]): Callback =
    cdm.modState(state => {
      ???
    })
}