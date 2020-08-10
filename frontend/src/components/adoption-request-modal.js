import React from 'react';
import {Button, Form, Header, Icon, Modal} from 'semantic-ui-react';
import * as PropTypes from "prop-types";
import {deleteAdoptionRequest, editAdoptionRequest, submitAdoptionRequest} from "../httpClient";
import {AppContext} from "../AppContext";

export default class AdoptionRequestModal extends React.Component {

    static contextType = AppContext;

    constructor(props, context) {
        super(props, context);
        this.state = {
            modalOpen: false,
            email: '',
            notes: '',
            error: '',
        };
    }

    handleOpen = () => this.setState({
        modalOpen: true,
        ...this.props.existingRequest,
    });

    handleClose = () => this.setState({modalOpen: false});

    handleChange = (e, {name, value}) => this.setState({[name]: value});

    handleDelete = async () => {
        try {
            await deleteAdoptionRequest({
                animalId: this.props.animal.id,
                adoptionRequestId: this.props.existingRequest.id,
            });

            this.closeModal();
        } catch (e) {
            this.setState({ error: JSON.stringify(e) });
        }
    };

    handleSubmit = async () => {
        try {
            if (this.props.existingRequest === undefined) {
                await submitAdoptionRequest({
                    animalId: this.props.animal.id,
                    email: this.state.email,
                    notes: this.state.notes,
                });
            } else {
                await editAdoptionRequest({
                    animalId: this.props.animal.id,
                    adoptionRequestId: this.props.existingRequest.id,
                    email: this.state.email,
                    notes: this.state.notes,
                });
            }
            this.closeModal();
        } catch (e) {
            this.setState({ error: JSON.stringify(e) });
        }
    };

    closeModal() {
        this.setState({
            email: '',
            notes: '',
            modalOpen: false,
        });

        this.context.refresh();
    }

    render() {
        const haveRequested = this.props.existingRequest !== undefined;
        const triggerButton = haveRequested ? (
            <Button basic color='blue'
                    disabled={!this.props.isSignedIn}
                    onClick={this.handleOpen}>
                Edit Adoption Request
            </Button>
        ) : (
            <Button basic color='green'
                    disabled={!this.props.isSignedIn}
                    onClick={this.handleOpen}>
                Adopt
            </Button>
        );

        const deleteButton = haveRequested ? (
            <Button color='red' onClick={this.handleDelete}>
                <Icon name='trash alternate outline'/> Delete Request
            </Button>
        ) : null;

        const errorMessage = this.state.error === '' ? (
            <div/>
        ) : (
            <div className="ui error message">
                <div className="content">
                    <div className="header">Action Forbidden</div>
                    <p>{this.state.error}</p>
                </div>
            </div>
        );

        return (
            <Modal trigger={triggerButton}
                   open={this.state.modalOpen}
                   onClose={this.handleClose}
                   size="small"
                   closeIcon>
                <Header icon='heartbeat' content={`Adopt ${this.props.animal.name}`}/>
                <Modal.Content>
                    <Form error={this.state.error !== ''}>
                        <Form.Input
                            placeholder='Name'
                            label='Email'
                            name='email'
                            value={this.state.email}
                            onChange={this.handleChange}
                        />
                        <Form.TextArea label='Notes'
                                       name='notes'
                                       placeholder={`Tell us why you like ${this.props.animal.name}...`}
                                       value={this.state.notes}
                                       onChange={this.handleChange}/>
                        {errorMessage}
                    </Form>
                </Modal.Content>
                <Modal.Actions>
                    <Button basic color='red' onClick={this.handleClose}>
                        <Icon name='remove'/> Cancel
                    </Button>
                    {deleteButton}
                    <Button color='green' onClick={this.handleSubmit}>
                        <Icon name='checkmark'/> Apply
                    </Button>
                </Modal.Actions>
            </Modal>
        );
    }
}

AdoptionRequestModal.propTypes = {
    animal: PropTypes.shape({
        id: PropTypes.number.isRequired,
        name: PropTypes.string.isRequired,
    }).isRequired,
    existingRequest: PropTypes.shape({
        id: PropTypes.number.isRequired,
        email: PropTypes.string,
        notes: PropTypes.string,
    }),
    isSignedIn: PropTypes.bool.isRequired,
};

