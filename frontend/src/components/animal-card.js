import React from 'react';
import * as PropTypes from 'prop-types';
import {Card, Icon, Image} from 'semantic-ui-react';
import AdoptionRequestModal from "./adoption-request-modal";


export default class AnimalCard extends React.Component {

    findExistingRequest() {
        return this.props.animal.adoptionRequests
            .find(request => request.adopterName === this.props.username);
    }

    render() {
        return (<Card>
                <Card.Content>
                    <Image
                        floated='right'
                        size='medium'
                        src={this.props.animal.avatarUrl}
                    />
                    <Card.Header>{this.props.animal.name}</Card.Header>
                    <Card.Meta>
                        <span className='date'>Rescued on {this.props.animal.rescueDate}</span>
                    </Card.Meta>
                    <Card.Description>
                        {this.props.animal.description}
                    </Card.Description>
                </Card.Content>
                <Card.Content extra>
                    <div className="pending">
                        <Icon name='user'/>
                        <span className="pending-number">{this.props.animal.adoptionRequests.length}</span>
                        <span> </span>Pending Adopters
                    </div>
                    <div className='ui two buttons'>
                        <AdoptionRequestModal animal={this.props.animal}
                                              existingRequest={this.findExistingRequest()}
                                              isSignedIn={this.props.username !== ''}
                         />
                    </div>
                </Card.Content>
            </Card>
        );
    }
}

AnimalCard.propTypes = {
    animal: PropTypes.shape({
        avatarUrl: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        rescueDate: PropTypes.string.isRequired,
        description: PropTypes.string.isRequired,
        adoptionRequests: PropTypes.arrayOf(PropTypes.shape({
            adopterName: PropTypes.string.isRequired,
        })).isRequired,
    }).isRequired,
    username: PropTypes.string.isRequired,
};

