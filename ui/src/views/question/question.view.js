import React from 'react';
import { t } from 'ttag';
import { withRouter } from 'react-router-dom';
import FillPage from '../../components/helpers/fillpage';
import Loader from '../../components/utils/loader';
import Markdown from '../../components/helpers/markdown';
import InputField from '../../components/forms/inputfield';
import Button from '../../components/forms/button';
import Question from '../../components/questions/question';
import Answer from '../../components/questions/answer';
import * as QuestionApi from '../../models/question';
import * as AnswerApi from '../../models/answer';
import { UserContext } from '../../usercontext';


class QuestionView extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loaded: false,
            question: null,
            answer: '',
            answers: [],
            postingAnswer: false,
            answerError: null,
            owned: false
        };
    }

    onChangeAnswer(node) {
        this.setState({
            answer: node.target.value
        });
    }

    componentDidMount() {
        const questionId = this.props.match.params.id;
        this.loadThreadAndAnswers(questionId);
    }

        loadThreadAndAnswers(questionId) {
        Promise.all([
            QuestionApi.getQuestionById(questionId),
            AnswerApi.getAnswersByQuestionId(questionId)
        ]).then((resp) => {
            this.setState({
                question: resp[0],
                owned: this.context.state.user.id === resp[0].userId,
                answers: resp[1],
                loaded: true
            });
        }).catch(() => {
            this.props.history.replace('../404');
        });
    }

    saveAnswer() {
        if (!this.state.answer || this.state.answer.trim().length <= 0) {
            this.setState({
                answerError: t`You cannot leave an empty answer.`
            });
            return;
        }

        this.setState({
            postingAnswer: true,
            answerError: null
        });

        const answer = {
            answer: this.state.answer
        };

        AnswerApi.answerQuestion(answer, this.props.match.params.id, this.context.state.token).then((resp) => {
            this.setState({
                answer: '',
                postingAnswer: false
            });
            this.loadThreadAndAnswers(this.props.match.params.id);
        });
    }

    renderAnswers() {
        return this.state.answers.map((answer, index) => {
            return <Answer onAnswer={this.onAnswerAccepted.bind(this)}
                           enableAccept={!this.state.question.answerAccepted && this.state.owned} answer={answer}
                           key={index} />;
        });
    }

    onAnswerAccepted(answer) {
        AnswerApi.acceptAnswer(answer.id).then(() => {
            this.loadThreadAndAnswers(this.props.match.params.id);
        });
    }

    renderAnswerForm() {
        if (this.state.postingAnswer) {
            return <Loader />;
        }

        return (
            <div className="answer-form">
                <div className="padded-bottom">
                    {t`Use Markdown in answer field.`} <a
                    href="https://guides.github.com/features/mastering-markdown/"
                    target="_blank">{t`Markdown-syntax`}</a>
                </div>
                <InputField
                    label={t`Answer`}
                    type="textarea"
                    name="answer"
                    value={this.state.answer}
                    onChange={this.onChangeAnswer.bind(this)}
                    errorMessage={this.state.answerError}
                />
                <Button color="secondary"
                        onClick={this.saveAnswer.bind(this)}>{t`Post answer`}</Button>
            </div>
        );

    }

    renderPreview() {
        if (!this.state.answer) {
            return null;
        }
        return (
            <div className="padded-vertical">
                <div className="underlined">{t`Preview`}</div>
                <Markdown text={this.state.answer} />
            </div>
        );
    }

    render() {
        if (!this.state.loaded) {
            return (
                <FillPage>
                    <Loader />
                </FillPage>
            );
        }

        return (
            <div className="content">
                <Question question={this.state.question} />
                <h3>{t`Answers`}</h3>
                {this.renderAnswers()}
                {this.renderAnswerForm()}
                {this.renderPreview()}
            </div>
        );
    }
}

const WrappedQuestionView                        = withRouter(QuestionView);
WrappedQuestionView.WrappedComponent.contextType = UserContext;
export default WrappedQuestionView;
