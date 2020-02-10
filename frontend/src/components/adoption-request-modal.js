import React from 'react'
import {Button, Form, Header, Icon, Modal} from 'semantic-ui-react'
import * as PropTypes from "prop-types";
import HttpClient from "../httpClient";

export default class AdoptionRequestModal extends React.Component {

    constructor(props, context) {
        super(props, context);
        this.state = {
            modalOpen: false,
            email: '',
            notes: '',
        };
    }

    handleOpen = () => this.setState({modalOpen: true});

    handleClose = () => this.setState({modalOpen: false});

    handleChange = (e, {name, value}) => this.setState({[name]: value});

    handleSubmit = async () => {
        await this.props.httpClient.submitAdoptionRequest({
            animalId: this.props.animal.id,
            email: this.state.email,
            notes: this.state.notes,
        });

        this.setState({
            email: '',
            notes: '',
            modalOpen: false,
        });

        window.location.reload(false);
    };

    render() {
        const triggerButton = (
            <Button basic color='green' onClick={this.handleOpen}>
                Adopt
            </Button>
        );

        return (
            <Modal trigger={triggerButton}
                   open={this.state.modalOpen}
                   onClose={this.handleClose}
                   size="small"
                   closeIcon>
                <Header icon='heartbeat' content={`Adopt ${this.props.animal.name}`}/>
                <Modal.Content>
                    <Form>
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
                    </Form>
                </Modal.Content>
                <Modal.Actions>
                    <Button basic color='red' onClick={this.handleClose}>
                        <Icon name='remove'/> Cancel
                    </Button>
                    <Button color='green' onClick={this.handleSubmit}>
                        <Icon name='checkmark'/> Apply
                    </Button>
                </Modal.Actions>
            </Modal>
        )
    }
}

AdoptionRequestModal.propTypes = {
    animal: PropTypes.shape({
        id: PropTypes.number,
        name: PropTypes.string,
    }),
    httpClient: PropTypes.instanceOf(HttpClient).isRequired,
};

